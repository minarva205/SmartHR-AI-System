package com.smarthr.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@CrossOrigin("*")
public class BrandingController {

    @Autowired
    private JdbcTemplate db;

    @GetMapping("/config")
    public Map<String, Object> getGlobalConfig() {
        return db.queryForMap("SELECT * FROM company_settings WHERE id = 1");
    }
}
