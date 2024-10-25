package com.IEEEUWUSB.IEEEStudentBranchBackEnd.config;
import com.IEEEUWUSB.IEEEStudentBranchBackEnd.entity.*;
import com.IEEEUWUSB.IEEEStudentBranchBackEnd.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Autowired
    DataSource dataSource;

    @Autowired
    public WalletService walletService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private PolicyService policyService;

    @Autowired

    private AcademicYearService academicYearService;

    @Autowired

    private RoleServices roleServices;

    @Autowired
    private UserService userService;

    private final PasswordEncoder passwordEncoder;

    public SecurityConfiguration(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/academic/**").permitAll()
                        .anyRequest().authenticated());
        http.sessionManagement(
                session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS)
        );
        http.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler));
        http.headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions
                        .sameOrigin()
                )
        );
        http.csrf(csrf -> csrf.disable());
        http.addFilterBefore(authenticationJwtTokenFilter(),
                UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }


    @Bean
    public CommandLineRunner initData(UserRoleDetailsServices userRoleDetailsServices) {
        return args -> {
            if (policyService.getPolicyBycode("OTHER") == null) {
                List<Policy> policies = new ArrayList<>();
                List<User> users = new ArrayList<>();
                var OtherPolicy = Policy.builder()
                        .policy("Other")
                        .policyCode("OTHER")
                        .type("MAIN").build();
                Policy savedPolicy = policyService.CreatePolicy(OtherPolicy);


                //policies
                policies.add(Policy.builder().policy("Finance").policyCode("FINANCE").type("MAIN").build());
                policies.add(Policy.builder().policy("Finance Transaction").policyCode("FINANCE_TRANSACTION").type("MAIN").build());
                policies.add(Policy.builder().policy("Finance budget").policyCode("FINANCE_BUDGET_PROPOSAL").type("MAIN").build());
                policies.add(Policy.builder().policy("Finance all").policyCode("FINANCE_ALL").type("MAIN").build());
                policies.add(Policy.builder().policy("Project").policyCode("PROJECT").type("MAIN").build());
                policies.add(Policy.builder().policy("Project").policyCode("PROJECT_ALL").type("MAIN").build());
                policies.add(Policy.builder().policy("Project_timeline").policyCode("PROJECT_TIME").type("MAIN").build());
                policies.add(Policy.builder().policy("Project_finance").policyCode("PROJECT_FINANCE").type("PROJECT").build());
                policies.add(Policy.builder().policy("Project_task").policyCode("PROJECT_TASK").type("PROJECT").build());
                policies.add(Policy.builder().policy("Project_assign").policyCode("PROJECT_ASSIGN").type("PROJECT").build());
                policies.add(Policy.builder().policy("Project_events").policyCode("PROJECT_EVENT").type("PROJECT").build());
                policies.add(Policy.builder().policy("Excom").policyCode("EXCOM").type("EXCOM").build());
                policies.add(Policy.builder().policy("Excom_All").policyCode("EXCOM_ALL").type("EXCOM").build());
                policies.add(Policy.builder().policy("Excom_task").policyCode("EXCOM_TASK").type("EXCOM").build());
                policies.add(Policy.builder().policy("Excom_task_assign").policyCode("EXCOM_TASK_ASSIGN").type("EXCOM").build());
                policies.add(Policy.builder().policy("Excom_assign").policyCode("EXCOM_ASSIGN").type("EXCOM").build());
                policies.add(Policy.builder().policy("service").policyCode("SERVICE").type("MAIN").build());
                policies.add(Policy.builder().policy("service_volunteer").policyCode("SERVICE_VOLUNTEER").type("MAIN").build());


                Wallet mainWallet = Wallet.builder()
                        .amount(0.0)
                        .type("MAIN")
                        .build();

                walletService.saveWallet(mainWallet);

                for (Policy policy : policies) {
                    Policy savedPolicies = policyService.CreatePolicy(policy);
                    System.out.println("Saved Policy: " + savedPolicy);
                }


                Set<Policy> otherpolicies = new HashSet<>();
                otherpolicies.add(savedPolicy);
                var userRole = Role.builder()
                        .userRole("Admin")
                        .type("MAIN")
                        .policies(otherpolicies)
                        .build();

                var userRoleMember = Role.builder()
                        .userRole("Member")
                        .type("MAIN")
                        .build();


                var memberUserRole = roleServices.CreateRole(userRoleMember);

                var AdminRole = roleServices.CreateRole(userRole);


                var AcedemicYear = AcademicYear.builder()
                        .academicYear("2024")
                        .enrolledBatch("15th Gen")
                        .status("ACTIVE")
                        .build();

                var savedAcedemicYear = academicYearService.createAcademicYear(AcedemicYear);


                var newAdmin = User.builder()
                        .email("aasadh2000@gmail.com")
                        .password(passwordEncoder.encode("12345678"))
                        .firstName("Mohamed")
                        .lastName("Aasath")
                        .contactNo("0755701765")
                        .createdDate(LocalDateTime.now())
                        .status("VERIFIED")
                        .academicYear(savedAcedemicYear)
                        .build();

                var savedUser = userService.saveUser(newAdmin);
                var userRoleDetails = UserRoleDetails.builder()
                        .user(savedUser)
                        .role(AdminRole)
                        .isActive(true)
                        .type(AdminRole.getType())
                        .start_date(LocalDateTime.now()).build();

                userRoleDetailsServices.createUserRoleDetails(userRoleDetails);

                //test users
                for (char c = 'a'; c <= 'z'; c++) {

                    users.add(User.builder().email(c + "@gmail.com").academicYear(savedAcedemicYear).password(passwordEncoder.encode("12345678")).firstName(c + "Mohamed").lastName(c + "Aasath").contactNo("0755701765").createdDate(LocalDateTime.now()).status("VERIFIED").build());

                }


                users.forEach(user -> {
                    var newuser = userService.saveUser(user);
                    var testuser = UserRoleDetails.builder()
                            .user(newuser)
                            .role(memberUserRole)
                            .isActive(true)
                            .type(memberUserRole.getType())
                            .start_date(LocalDateTime.now()).build();

                    userRoleDetailsServices.createUserRoleDetails(testuser);
                });
            }

        };
    }


}
