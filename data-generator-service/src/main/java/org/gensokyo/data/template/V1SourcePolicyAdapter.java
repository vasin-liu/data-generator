package org.gensokyo.data.template;

import org.gensokyo.data.model.v2.SourcePolicyVO;
import org.gensokyo.data.model.vo.selector.value.MultipleOrderValueSelectStrategyVO;
import org.gensokyo.data.model.vo.selector.value.ValueSelectStrategyVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.model.vo.stage.SelectStageVO;

public final class V1SourcePolicyAdapter {
    private V1SourcePolicyAdapter() {
    }

    public static SourcePolicyVO fromStages(ReadStageVO readStage, SelectStageVO selectStage) {
        SourcePolicyVO policy = new SourcePolicyVO();
        boolean mapped = false;

        if (readStage != null) {
            policy.setInMemory(readStage.isInMemory());
            mapped = readStage.isInMemory();
        }

        if (selectStage != null && selectStage.getStrategy() != null) {
            ValueSelectStrategyVO strategy = selectStage.getStrategy();
            if (strategy.getType() != null && !strategy.getType().isBlank()) {
                policy.setSelectionStrategy(strategy.getType());
                mapped = true;
            }
            Integer limit = selectLimit(strategy);
            if (limit != null && limit > 0) {
                policy.setLimit(limit);
                mapped = true;
            }
        }

        return mapped ? policy : null;
    }

    private static Integer selectLimit(ValueSelectStrategyVO strategy) {
        if (strategy == null) {
            return null;
        }
        if (strategy instanceof MultipleOrderValueSelectStrategyVO multipleOrder) {
            int selectNum = positive(strategy.getSelectNum());
            int maxTimes = positive(multipleOrder.getMaxTimes());
            if (selectNum <= 0 && maxTimes <= 0) {
                return null;
            }
            return (selectNum <= 0 ? 1 : selectNum) * (maxTimes <= 0 ? 1 : maxTimes);
        }
        int selectNum = positive(strategy.getSelectNum());
        return selectNum > 0 ? selectNum : null;
    }

    private static int positive(int value) {
        return Math.max(value, 0);
    }
}
