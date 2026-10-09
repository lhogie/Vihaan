package fr.lab.hr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.lab.hr.model.Pays;

public interface PaysRepository extends JpaRepository<Pays, Long>, JpaSpecificationExecutor<Pays> {
}
