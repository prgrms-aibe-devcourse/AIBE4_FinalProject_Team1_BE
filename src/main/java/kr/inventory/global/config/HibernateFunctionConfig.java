package kr.inventory.global.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

public class HibernateFunctionConfig implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
                "word_similarity",
                "word_similarity(?1, ?2)",
                functionContributions.getTypeConfiguration().getBasicTypeRegistry()
                        .resolve(StandardBasicTypes.DOUBLE)
        );

        functionContributions.getFunctionRegistry().registerPattern(
                "similarity",
                "similarity(?1, ?2)",
                functionContributions.getTypeConfiguration().getBasicTypeRegistry()
                        .resolve(StandardBasicTypes.DOUBLE)
        );
    }
}
