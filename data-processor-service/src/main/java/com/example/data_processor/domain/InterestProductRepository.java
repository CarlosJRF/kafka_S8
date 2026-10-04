package com.example.data_processor.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestProductRepository extends JpaRepository<InterestProduct, String> {

	List<InterestProduct> findByCuentaIdOrderBySourceId(int cuentaId);

}
