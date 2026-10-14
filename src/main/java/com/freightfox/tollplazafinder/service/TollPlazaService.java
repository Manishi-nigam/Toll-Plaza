package com.freightfox.tollplazafinder.service;

import com.freightfox.tollplazafinder.entity.TollPlaza;
import com.freightfox.tollplazafinder.repository.TollPlazaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TollPlazaService {

    private final TollPlazaRepository tollPlazaRepository;

    public TollPlazaService(TollPlazaRepository tollPlazaRepository){
        this.tollPlazaRepository= tollPlazaRepository;
    }

    public List<TollPlaza> getAllTollPlazas(){
        return tollPlazaRepository.findAll();
    }
}
