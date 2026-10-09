package fr.lab.hr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.lab.hr.model.Employeur;

public interface EmployeurRepository extends JpaRepository<Employeur, Long>, JpaSpecificationExecutor<Employeur> {
}
