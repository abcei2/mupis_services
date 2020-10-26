package com.intertelco.screen.screenserver.jpa.repositories;

import com.intertelco.screen.screenserver.jpa.entities.Screen;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenRepository extends CrudRepository<Screen, String> {

}
