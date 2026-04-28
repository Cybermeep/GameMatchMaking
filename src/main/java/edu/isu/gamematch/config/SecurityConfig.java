package edu.isu.gamematch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // Disable CSRF for now (re-enable later with proper tokens)
            .authorizeRequests()
                .antMatchers("/", "/index", "/auth/**", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                .anyRequest().permitAll()  // Allow all requests for now
            .and()
                .logout()
                .logoutSuccessUrl("/")
                .permitAll();
    }
}