package io.github.yush1x.tenjudge.server.auth;

import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequestDTO;
import io.github.yush1x.tenjudge.server.auth.persistence.UsersUpdateService;
import io.github.yush1x.tenjudge.server.auth.service.AuthChecker;
import io.github.yush1x.tenjudge.server.auth.service.RequestChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthServiceTest {

    @Mock
    AuthChecker authChecker;

    @Mock
    UsersUpdateService usersUpdateService;

    @Mock
    RequestChecker requestChecker;

    @InjectMocks
    AuthService authService;

    @Test
    public void register_adminOperation() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setRole("super_admin");
        request.setUsername("admin");
        request.setPassword("admin123123");
        request.setEmail("123123@qq.com");

        when(authChecker.checkAdmin()).thenReturn(1L);
        when(usersUpdateService.insert(any())).thenReturn(5L);

        authService.register(request);
    }

}
