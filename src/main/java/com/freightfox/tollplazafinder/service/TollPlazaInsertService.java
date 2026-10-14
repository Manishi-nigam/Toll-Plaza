package com.freightfox.tollplazafinder.service;


import com.freightfox.tollplazafinder.entity.TollPlaza;
import com.freightfox.tollplazafinder.repository.TollPlazaRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class TollPlazaInsertService implements CommandLineRunner {

    private final TollPlazaRepository tollPlazaRepository;

    public TollPlazaInsertService(TollPlazaRepository tollPlazaRepository){
        this.tollPlazaRepository=tollPlazaRepository;
    }

    @Override
    public void run(String ... args) throws Exception{
        if (tollPlazaRepository.count()>0){
            System.out.println("Toll Plaza data already exists. Skipping import.");
            return;
        }

        ClassPathResource resource=  new ClassPathResource("data/toll_plaza_india.csv");

        List<TollPlaza> tollPlazas = new ArrayList<>();

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                resource.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );

                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .get()
                        .parse(reader)
        ) {
            for (CSVRecord record : parser) {

                TollPlaza tollPlaza = new TollPlaza();

                tollPlaza.setLongitude(
                        Double.parseDouble(record.get("longitude").trim())
                );

                tollPlaza.setLatitude(
                        Double.parseDouble(record.get("latitude").trim())
                );

                tollPlaza.setTollName(
                        record.get("toll_name").trim()
                );

                tollPlaza.setGeoState(
                        record.get("geo_state").trim()
                );

                tollPlazas.add(tollPlaza);
            }
        }

        int batchSize = 100;

        for (int i = 0; i < tollPlazas.size(); i += batchSize) {
            int end = Math.min(i + batchSize, tollPlazas.size());

            List<TollPlaza> batch = tollPlazas.subList(i, end);

            tollPlazaRepository.saveAll(batch);

            System.out.println(
                    "Saved " + end + " out of " + tollPlazas.size() + " toll plazas."
            );
        }

        System.out.println(
                "Successfully imported " + tollPlazas.size() + " toll plazas."
        );
    }
}
