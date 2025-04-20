package com.example.atheer_ct.repo;

import com.example.atheer_ct.entities.Tower;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TowerRepository extends CrudRepository<Tower, Long> {
    Tower findByTawalId(String tawalId);
    List<Tower> findAll();
    Tower findBySiteName(String siteName);
    Tower findByLatitudeAndLongitude(double latitude, double longitude);
    Tower findByTotalHeight(int totalHeight);
    Tower findByPower(String power);
    Tower findByClutter(String clutter);
    Tower findByTawalIdAndSiteName(String tawalId, String siteName);

}
