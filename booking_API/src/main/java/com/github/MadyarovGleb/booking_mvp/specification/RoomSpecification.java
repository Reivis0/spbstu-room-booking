package com.github.MadyarovGleb.booking_mvp.specification;

import com.github.MadyarovGleb.booking_mvp.entity.Room;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RoomSpecification {

    public static Specification<Room> withFilters(
            UUID buildingId,
            String university,
            Integer capacityMin,
            Integer capacityMax,
            List<String> features,
            String searchStr
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by building_id
            if (buildingId != null) {
                predicates.add(criteriaBuilder.equal(root.get("buildingId"), buildingId));
            }

            // Filter by university code
            if (university != null && !university.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("universityCode"), university));
            }

            // Filter by minimum capacity
            if (capacityMin != null && capacityMin > 0) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("capacity"), capacityMin));
            }

            // Filter by maximum capacity
            if (capacityMax != null && capacityMax > 0) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("capacity"), capacityMax));
            }

            // Filter by features (JSON array contains check)
            if (features != null && !features.isEmpty()) {
                for (String feature : features) {
                    // PostgreSQL jsonb_array_elements_text function to check if feature exists
                    predicates.add(criteriaBuilder.isTrue(
                            criteriaBuilder.function(
                                    "jsonb_exists",
                                    Boolean.class,
                                    root.get("features"),
                                    criteriaBuilder.literal(feature)
                            )
                    ));
                }
            }

            // Filter by search string (name or number)
            if (searchStr != null && !searchStr.isBlank()) {
                String searchPattern = "%" + searchStr.toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        searchPattern
                );
                Predicate numberPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("number")),
                        searchPattern
                );
                predicates.add(criteriaBuilder.or(namePredicate, numberPredicate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
