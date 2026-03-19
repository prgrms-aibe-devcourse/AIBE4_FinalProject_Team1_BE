package kr.inventory.domain.reference.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.inventory.domain.reference.repository.MenuRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class MenuRepositoryImpl implements MenuRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean existsActiveMenuUsingIngredient(Long storeId, UUID ingredientPublicId) {
        String sql = """
                select exists (
                    select 1
                    from menus m
                    where m.store_id = :storeId
                      and m.status = 'ACTIVE'
                      and exists (
                          select 1
                          from jsonb_array_elements(m.ingredients_json) elem
                          where elem ->> 'ingredientPublicId' = :ingredientPublicId
                      )
                )
                """;

        Object result = em.createNativeQuery(sql)
                .setParameter("storeId", storeId)
                .setParameter("ingredientPublicId", ingredientPublicId.toString())
                .getSingleResult();

        return result instanceof Boolean b && b;
    }
}