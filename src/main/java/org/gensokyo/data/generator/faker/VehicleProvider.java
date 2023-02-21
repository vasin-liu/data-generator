/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.faker;

import com.google.common.base.Joiner;
import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import org.gensokyo.data.generator.constant.Const;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.kit.character.StrKit;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.gensokyo.data.generator.util.RandomKit.choiceOne;


/**
 * 车辆假数据生成器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/1 , Version 1.0.0
 */
public class VehicleProvider extends AbstractProvider<BaseProviders> {
    private static final String ROOT = "vehicle";
    private static final String PROVINCES = "provinces";
    private static final List<String> letter = IntStream.rangeClosed('A', 'Z')
            .filter(value -> !List.of('I', 'O').contains((char) value))
            .mapToObj(value -> String.valueOf((char) value)).toList();
    private static final List<String> numeric = IntStream.rangeClosed(0, 9).mapToObj(String::valueOf).toList();
    private static final List<String> letterAndNumeric = Stream.of(letter, numeric).flatMap(Collection::stream).toList();

    private static final Map<String, String> plateProvince = new HashMap<>();

    static {
        plateProvince.put("皖", "1");
        plateProvince.put("京", "2");
        plateProvince.put("闽", "3");
        plateProvince.put("甘", "4");
        plateProvince.put("粤", "5");
        plateProvince.put("桂", "6");
        plateProvince.put("贵", "7");
        plateProvince.put("琼", "8");
        plateProvince.put("冀", "9");
        plateProvince.put("豫", "10");
        plateProvince.put("黑", "11");
        plateProvince.put("鄂", "12");
        plateProvince.put("湘", "13");
        plateProvince.put("吉", "14");
        plateProvince.put("苏", "15");
        plateProvince.put("赣", "16");
        plateProvince.put("辽", "17");
        plateProvince.put("蒙", "18");
        plateProvince.put("宁", "19");
        plateProvince.put("青", "20");
        plateProvince.put("鲁", "21");
        plateProvince.put("晋", "22");
        plateProvince.put("陕", "23");
        plateProvince.put("沪", "24");
        plateProvince.put("川", "25");
        plateProvince.put("津", "26");
        plateProvince.put("藏", "27");
        plateProvince.put("新", "28");
        plateProvince.put("云", "29");
        plateProvince.put("浙", "30");
        plateProvince.put("渝", "31");
        plateProvince.put("澳", "88");
        plateProvince.put("港", "89");
    }

    public VehicleProvider(BaseProviders faker) {
        super(faker);
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            var resource = resolver.getResource("classpath:faker/vehicle.yaml");
            faker.addPath(Locale.CHINA, resource.getFile().toPath());
        } catch (IOException e) {
            throw new DataGeneratorException(e);
        }
    }

    private String letterAndNumeric(int length) {
        var p = new StringBuilder();
        for (int i = 0; i < length; i++) {
            p.append(choiceOne(letterAndNumeric));
        }
        return p.toString();
    }

    private String numeric(int length) {
        var p = new StringBuilder();
        for (int i = 0; i < length; i++) {
            p.append(choiceOne(numeric));
        }
        return p.toString();
    }

    private String key(String... keys) {
        return ROOT.concat(Const.DOT).concat(Joiner.on(Const.DOT).join(keys));
    }

    public String normal() {
        return resolve(key(PROVINCES)) + choiceOne(letter) + letterAndNumeric(5);
    }

    public String macau() {
        return "粤Z" + letterAndNumeric(4) + "澳";
    }

    public String hongkong() {
        return "粤Z" + letterAndNumeric(4) + "港";
    }

    public String police() {
        return resolve(key(PROVINCES)) + choiceOne(letter) + numeric(4) + "警";
    }

    public String coach() {
        return resolve(key(PROVINCES)) + choiceOne(letter) + numeric(4) + "学";
    }

    public String trailer() {
        return resolve(key(PROVINCES)) + choiceOne(letter) + numeric(4) + "挂";
    }

    public String plate(String type) {
        return switch (Type.of(type)) {
            case NORMAL_BLUE, NORMAL_YELLOW -> normal();
            case HONGKONG -> hongkong();
            case MACAU -> macau();
            case POLICE -> police();
            case COACH -> coach();
            case TRAILER -> trailer();
        };
    }

    public String plateProvince(String plate) {
        if (StrKit.isBlank(plate)) {
            return StrKit.EMPTY;
        }
        if (StrKit.startWith(plate, "粤Z")) {
            return plateProvince.get(plate.substring(plate.length() - 1));
        }
        var p = plate.substring(0, 1);
        if (plateProvince.containsKey(p)) {
            return plateProvince.get(p);
        }
        return StrKit.EMPTY;
    }

    enum Type {
        //# 1 普通小车；2 普通大车；3 香港车；4 澳门车；5 教练车；6 警车；7 挂车；
        NORMAL_BLUE("1"), NORMAL_YELLOW("2"), HONGKONG("3"), MACAU("4"), COACH("5"), POLICE("6"), TRAILER("7");

        private final String code;

        Type(String code) {
            this.code = code;
        }

        String code() {
            return this.code;
        }

        public static Type of(String type) {
            return Arrays.stream(values()).filter(e -> e.code().equals(type)).findFirst().orElse(NORMAL_BLUE);
        }
    }
}
