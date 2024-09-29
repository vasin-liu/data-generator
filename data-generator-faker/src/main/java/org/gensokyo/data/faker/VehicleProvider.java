/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.faker;

import net.datafaker.providers.base.AbstractProvider;
import net.datafaker.providers.base.BaseProviders;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.kit.character.StrKit;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * 车辆假数据生成器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/2/1 , Version 1.0.0
 */
public class VehicleProvider extends AbstractProvider<BaseProviders> {
    private static final List<String> PROVINCES = Arrays.asList("京", "津", "冀", "晋", "蒙", "辽", "吉", "黑", "沪", "苏",
            "浙", "皖", "闽", "赣", "鲁", "豫", "鄂", "湘", "粤", "桂", "琼", "渝", "川", "贵", "云", "藏", "陕", "甘", "青", "宁", "新");
    private static final List<String> LETTER = IntStream.rangeClosed('A', 'Z')
            .filter(value -> !List.of('I', 'O').contains((char) value))
            .mapToObj(value -> String.valueOf((char) value)).toList();
    private static final List<String> NUMERIC = IntStream.rangeClosed(0, 9).mapToObj(String::valueOf).toList();
    private static final List<String> LETTER_AND_NUMERIC = Stream.of(LETTER, NUMERIC).flatMap(Collection::stream).toList();

    private static final Map<String, String> PLATE_PROVINCE = new HashMap<>();

    static {
        PLATE_PROVINCE.put("皖", "1");
        PLATE_PROVINCE.put("京", "2");
        PLATE_PROVINCE.put("闽", "3");
        PLATE_PROVINCE.put("甘", "4");
        PLATE_PROVINCE.put("粤", "5");
        PLATE_PROVINCE.put("桂", "6");
        PLATE_PROVINCE.put("贵", "7");
        PLATE_PROVINCE.put("琼", "8");
        PLATE_PROVINCE.put("冀", "9");
        PLATE_PROVINCE.put("豫", "10");
        PLATE_PROVINCE.put("黑", "11");
        PLATE_PROVINCE.put("鄂", "12");
        PLATE_PROVINCE.put("湘", "13");
        PLATE_PROVINCE.put("吉", "14");
        PLATE_PROVINCE.put("苏", "15");
        PLATE_PROVINCE.put("赣", "16");
        PLATE_PROVINCE.put("辽", "17");
        PLATE_PROVINCE.put("蒙", "18");
        PLATE_PROVINCE.put("宁", "19");
        PLATE_PROVINCE.put("青", "20");
        PLATE_PROVINCE.put("鲁", "21");
        PLATE_PROVINCE.put("晋", "22");
        PLATE_PROVINCE.put("陕", "23");
        PLATE_PROVINCE.put("沪", "24");
        PLATE_PROVINCE.put("川", "25");
        PLATE_PROVINCE.put("津", "26");
        PLATE_PROVINCE.put("藏", "27");
        PLATE_PROVINCE.put("新", "28");
        PLATE_PROVINCE.put("云", "29");
        PLATE_PROVINCE.put("浙", "30");
        PLATE_PROVINCE.put("渝", "31");
        PLATE_PROVINCE.put("澳", "88");
        PLATE_PROVINCE.put("港", "89");
    }

    public VehicleProvider(BaseProviders faker) {
        super(faker);
    }

    private String letterAndNumeric(int length) {
        var p = new StringBuilder();
        for (int i = 0; i < length; i++) {
            p.append(RandomKit.choiceOne(LETTER_AND_NUMERIC));
        }
        return p.toString();
    }

    private String numeric(int length) {
        var p = new StringBuilder();
        for (int i = 0; i < length; i++) {
            p.append(RandomKit.choiceOne(NUMERIC));
        }
        return p.toString();
    }

    public String normal() {
        return RandomKit.choiceOne(PROVINCES) + RandomKit.choiceOne(LETTER) + letterAndNumeric(5);
    }

    public String macau() {
        return "粤Z" + letterAndNumeric(4) + "澳";
    }

    public String hongkong() {
        return "粤Z" + letterAndNumeric(4) + "港";
    }

    public String police() {
        return RandomKit.choiceOne(PROVINCES) + RandomKit.choiceOne(LETTER) + numeric(4) + "警";
    }

    public String coach() {
        return RandomKit.choiceOne(PROVINCES) + RandomKit.choiceOne(LETTER) + numeric(4) + "学";
    }

    public String trailer() {
        return RandomKit.choiceOne(PROVINCES) + RandomKit.choiceOne(LETTER) + numeric(4) + "挂";
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
            return PLATE_PROVINCE.get(plate.substring(plate.length() - 1));
        }
        var p = plate.substring(0, 1);
        if (PLATE_PROVINCE.containsKey(p)) {
            return PLATE_PROVINCE.get(p);
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
