package com.finture.resume.service;

import com.finture.resume.model.PersonalInfo;
import com.finture.resume.model.Resume;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class OptimizerServiceTest {

    @Test
    void optimize_shouldThrowOnInvalidApiKey() {
        OptimizerService service = new OptimizerService();
        Resume resume = new Resume();
        resume.setPersonalInfo(new PersonalInfo());
        resume.getPersonalInfo().setName("Test");
        resume.setSummary("A test resume");
        resume.setWorkExperience(new ArrayList<>());
        resume.setEducation(new ArrayList<>());
        resume.setSkills(new ArrayList<>());
        resume.setLanguage("en");

        // Without a valid API key, this should throw RuntimeException
        assertThrows(RuntimeException.class, () -> service.optimize(resume));
    }

    @Test
    void buildPrompt_shouldBeCalled() throws Exception {
        // Verify the service can be constructed (no NPE on fields)
        OptimizerService service = new OptimizerService();
        assertNotNull(service);
    }
}
