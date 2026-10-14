package com.freightfox.tollplazafinder.repository;

import com.freightfox.tollplazafinder.entity.TollPlaza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TollPlazaRepository  extends JpaRepository<TollPlaza, Long> {

}
