package com.example.data_processor.domain;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RejectedRowRepository extends JpaRepository<RejectedRow, String> {

	@Query("select r.file as file, count(r) as cantidad from RejectedRow r group by r.file order by r.file")
	List<CountByFile> countByFile();

	List<RejectedRow> findByFileOrderByLine(String file, Pageable pageable);

	interface CountByFile {

		String getFile();

		long getCantidad();

	}

}
