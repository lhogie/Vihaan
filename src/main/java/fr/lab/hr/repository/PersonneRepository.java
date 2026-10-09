package fr.lab.hr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.lab.hr.model.Personne;

public interface PersonneRepository extends JpaRepository<Personne, Long>, JpaSpecificationExecutor<Personne> {
}
