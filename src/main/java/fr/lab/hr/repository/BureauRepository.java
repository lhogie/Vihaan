package fr.lab.hr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.lab.hr.model.Bureau;

public interface BureauRepository extends JpaRepository<Bureau, Long>, JpaSpecificationExecutor<Bureau> {
}
