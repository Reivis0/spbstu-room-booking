package com.github.MadyarovGleb.booking_mvp.specification;

import com.github.MadyarovGleb.booking_mvp.entity.Building;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for filtering Building entities.
 */
public class BuildingSpecification {

    /**
     * Creates a Specification for filtering buildings by university code.
     * 
     * @param university University code (e.g., "spbptu", "spbgu", "leti")
     * @return Specification for Building entity
     */
    public static Specification<Building> withFilters(String university) {
        return (Root<Building> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by university code
            if (university != null && !university.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("universityCode"), university));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
