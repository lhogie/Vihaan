package fr.lab.hr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.lab.hr.model.Contrat;

public interface ContratRepository extends JpaRepository<Contrat, Long>, JpaSpecificationExecutor<Contrat> {
}
