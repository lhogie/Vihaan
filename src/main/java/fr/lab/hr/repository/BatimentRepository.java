package fr.lab.hr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.lab.hr.model.Batiment;

public interface BatimentRepository extends JpaRepository<Batiment, Long>, JpaSpecificationExecutor<Batiment> {
}
