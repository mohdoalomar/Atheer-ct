package com.example.atheer_ct.services;

import com.example.atheer_ct.repo.TowerRepository;
import org.springframework.stereotype.Service;

@Service
public class TowerService {
    private final TowerRepository towerRepository;

    public TowerService(TowerRepository towerRepository) {
        this.towerRepository = towerRepository;
    }

}
