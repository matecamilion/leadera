package com.leadera.leadera;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Perfil test (H2 en memoria): el contexto levanta sin depender de la
// MySQL local ni de variables de entorno.
@SpringBootTest
@ActiveProfiles("test")
class LeaderaApplicationTests {

	@Test
	void contextLoads() {
	}

}
