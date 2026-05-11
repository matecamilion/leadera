package com.leadera.leadera.service;

import com.leadera.leadera.model.Agente;
import com.leadera.leadera.repository.AgenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private final AgenteRepository agenteRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return agenteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Agente no encontrado con email: " + email));
    }
}
