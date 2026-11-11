package com.perkmanager.perk_manager.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void homepageRenders() {
        assertThat(mvc.get().uri("/"))
            .hasStatusOk()
            .hasViewName("index")
            .bodyText().contains("Perk Manager");
    }
}
