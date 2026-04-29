/*
 * Copyright 漏 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address锛歅CI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou锛孋hina锛圸ip code锛?10653锛?
 */
package org.gensokyo.data.writer;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.util.DatasetKit;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Postgres鏂囦欢娴佸啓鍏ュ櫒
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class PostgresWriter<S extends WriteStageVO, T extends PostgresWriterVO> implements Writer<S, T> {

    private static final String SQL_TEMPLATE = "COPY %s (%s) FROM STDIN WITH (FORMAT TEXT, ENCODING 'UTF-8', DELIMITER '"
            + Const.VERTICAL + "',NULL '" + Const.NULL + "', HEADER false)";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public long write(final StageContext<S> ctx, final T wvo, final List<Map<String, Object>> dataset) {
        Connection conn = null;
        try {
            DynamicDataSourceContextHolder.push(Objects.requireNonNull(wvo.getDataSourceId()));
            //閫氳繃jdbcTemplate.getDataSource().getConnection()鎬绘槸鍒涘缓鏂扮殑閾炬帴锛屼笉浼氬鐢ㄥ凡鏈夐摼鎺ワ紝鍥犳濡傛灉涓嶆墜鍔ㄥ叧闂垯浼氶€犳垚閾炬帴娉勯湶
            //DataSourceUtils.getConnection()瀹冮鍏堟煡鐪嬪綋鍓嶆槸鍚﹀瓨鍦ㄤ簨鍔＄鐞嗕笂涓嬫枃锛屽苟灏濊瘯浠庝簨鍔＄鐞嗕笂涓嬫枃鑾峰彇杩炴帴锛屽鏋滆幏鍙栧け璐ワ紝
            //鐩存帴浠庢暟鎹簮涓幏鍙栬繛鎺ャ€傚湪鑾峰彇杩炴帴鍚庯紝濡傛灉褰撳墠鎷ユ湁浜嬪姟涓婁笅鏂囷紝鍒欏皢杩炴帴缁戝畾鍒颁簨鍔′笂涓嬫枃涓€傚鏋滃浜庝簨鍔′笂涓嬫枃涓紝
            //閭ｄ箞涓嶉渶瑕佹樉绀哄叧闂垨鑰呴噴鏀捐繛鎺ワ紝浣嗘槸濡傛灉 DataSourceUtils 鍦ㄦ病鏈変簨鍔′笂涓嬫枃鐨勬柟娉曚腑浣跨敤 getConnection() 鑾峰彇杩炴帴锛?
            //渚濈劧浼氶€犳垚鏁版嵁杩炴帴娉勬紡锛屽洜姝ら渶瑕佹墜鍔ㄩ噴鏀撅細DataSourceUtils.releaseConnection(conn,jdbcTemplate.getDataSource())
            conn = DataSourceUtils.getConnection(Objects.requireNonNull(jdbcTemplate.getDataSource()));
            if (conn.isWrapperFor(BaseConnection.class)) {
                BaseConnection bc = conn.unwrap(BaseConnection.class);
                CopyManager copyManager = new CopyManager(bc);
                var sql = String.format(SQL_TEMPLATE, wvo.getTarget(), wvo.getTemplate());
                return copyManager.copyIn(sql, DatasetKit.buildBulkData(wvo.getTemplate(), dataset));
            }
        } catch (Exception e) {
            throw new DataGeneratorException(String.format(
                    "写入数据集时发生异常，数据库类型为：%s，数据源编号为：%s，目标表名为：%s，写入模板为：%s。",
                    wvo.getType(), wvo.getDataSourceId(), wvo.getTarget(), wvo.getTemplate()), e);
        } finally {
            DataSourceUtils.releaseConnection(conn, jdbcTemplate.getDataSource());
            DynamicDataSourceContextHolder.clear();
        }
        return 0L;

    }

}
