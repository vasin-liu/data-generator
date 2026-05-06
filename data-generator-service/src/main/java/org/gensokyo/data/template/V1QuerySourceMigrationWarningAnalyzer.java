package org.gensokyo.data.template;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.model.vo.FieldVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.selector.reader.ReaderSelectStrategyVO;
import org.gensokyo.data.model.vo.selector.value.ValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.reader.JdbcReaderVO;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class V1QuerySourceMigrationWarningAnalyzer {
    private V1QuerySourceMigrationWarningAnalyzer() {
    }

    public static List<String> analyze(TemplateVO template) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (template == null || template.getFields() == null) {
            return new ArrayList<>(warnings);
        }
        for (FieldVO field : template.getFields()) {
            if (field == null || field.getStages() == null) {
                continue;
            }
            String fieldName = fieldName(field);
            for (int i = 0; i < field.getStages().size(); i++) {
                StageVO stage = field.getStages().get(i);
                if (!(stage instanceof ReadStageVO readStage)) {
                    continue;
                }
                warnReaderStrategy(fieldName, readStage, warnings);
                if (i + 1 < field.getStages().size() && field.getStages().get(i + 1) instanceof SelectStageVO selectStage) {
                    warnSelectStrategy(fieldName, selectStage, warnings);
                }
            }
        }
        return new ArrayList<>(warnings);
    }

    private static void warnReaderStrategy(String fieldName,
                                           ReadStageVO readStage,
                                           LinkedHashSet<String> warnings) {
        if (readStage.getReaders() == null || readStage.getReaders().size() < 2 || !containsJdbcReader(readStage.getReaders())) {
            return;
        }
        String strategyType = readerStrategyType(readStage.getStrategy());
        if (Const.ReaderSelectStrategyType.EQUAL.equals(strategyType) || Const.ReaderSelectStrategyType.WEIGHT.equals(strategyType)) {
            warnings.add(String.format(
                    "Field '%s' uses V1 reader strategy '%s' across multiple readers. The QuerySourceVO draft does not preserve reader-pool dispatch semantics exactly.",
                    fieldName,
                    strategyType
            ));
        }
    }

    private static void warnSelectStrategy(String fieldName,
                                           SelectStageVO selectStage,
                                           LinkedHashSet<String> warnings) {
        if (selectStage == null || selectStage.getStrategy() == null) {
            return;
        }
        ValueSelectStrategyVO strategy = selectStage.getStrategy();
        String strategyType = valueStrategyType(strategy);
        switch (strategyType) {
            case Const.ValueSelectStrategyType.ONCE_ORDER ->
                    warnings.add(String.format(
                            "Field '%s' uses V1 select strategy 'ONCE_ORDER'. SourcePolicyVO keeps ordered materialization plus limit, but exact once/depletion semantics are not preserved.",
                            fieldName
                    ));
            case Const.ValueSelectStrategyType.ONCE_RANDOM ->
                    warnings.add(String.format(
                            "Field '%s' uses V1 select strategy 'ONCE_RANDOM'. SourcePolicyVO keeps shuffled materialization plus limit, but exact once/depletion semantics are not preserved.",
                            fieldName
                    ));
            case Const.ValueSelectStrategyType.MULTIPLE_ORDER ->
                    warnings.add(String.format(
                            "Field '%s' uses V1 select strategy 'MULTIPLE_ORDER'. SourcePolicyVO only keeps an approximate upper-bound limit; exact repeated-use semantics are not preserved.",
                            fieldName
                    ));
            default -> {
            }
        }
    }

    private static boolean containsJdbcReader(List<ReaderVO> readers) {
        for (ReaderVO reader : readers) {
            if (reader instanceof JdbcReaderVO) {
                return true;
            }
        }
        return false;
    }

    private static String fieldName(FieldVO field) {
        if (field.getName() == null || field.getName().isBlank()) {
            return "jdbc_reader";
        }
        return field.getName();
    }

    private static String readerStrategyType(ReaderSelectStrategyVO strategy) {
        if (strategy == null || strategy.getType() == null || strategy.getType().isBlank()) {
            return Const.ReaderSelectStrategyType.EQUAL;
        }
        return strategy.getType().trim().toUpperCase(Locale.ROOT);
    }

    private static String valueStrategyType(ValueSelectStrategyVO strategy) {
        if (strategy == null || strategy.getType() == null || strategy.getType().isBlank()) {
            return Const.ValueSelectStrategyType.REPEAT_RANDOM;
        }
        return strategy.getType().trim().toUpperCase(Locale.ROOT);
    }
}
