package fr.lab.hr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.lab.hr.model.Campus;

public interface CampusRepository extends JpaRepository<Campus, Long>, JpaSpecificationExecutor<Campus> {
}
