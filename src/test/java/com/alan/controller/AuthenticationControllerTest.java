package com.alan.controller;

import com.alan.common.TimeProvider;
import com.alan.model.Authority;
import com.alan.model.User;
import com.alan.security.DeviceDummy;
import com.alan.security.TokenHelper;
import org.assertj.core.util.DateUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mobile.device.Device;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Created by fanjin on 2017-09-01.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class AuthenticationControllerTest {

    private MockMvc mvc;

    @Mock
    private TimeProvider timeProviderMock;

    private static final String TEST_USERNAME = "testUser";

    @InjectMocks
    private TokenHelper tokenHelper;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private WebApplicationContext context;

    @InjectMocks
    DeviceDummy device;

    @Before
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        User user = new User();
        user.setUsername("username");
        Authority authority = new Authority();
        authority.setId(0L);
        authority.setName("ROLE_USER");
        List<Authority> authorities = Arrays.asList(authority);
        user.setAuthorities(authorities);
        when(this.userDetailsService.loadUserByUsername(eq("testUser"))).thenReturn(user);

        ReflectionTestUtils.setField(tokenHelper, "EXPIRES_IN", 10L); // 10 sec
        ReflectionTestUtils.setField(tokenHelper, "MOBILE_EXPIRES_IN", 20L); // 20 sec
        ReflectionTestUtils.setField(tokenHelper, "SECRET", "mySecret");
    }

    @Test
    @WithMockUser(roles = "USER")
    public void shouldGetEmptyTokenStateWhenGivenValidOldToken() throws Exception {
        when(timeProviderMock.now())
                .thenReturn(DateUtil.yesterday());
        this.mvc.perform(get("/auth/refresh"))
                .andExpect(content().json("{access_token:null,expires_in:null}"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void shouldNotRefreshExpiredWebToken() throws Exception {
        Date beforeSomeTime = new Date(DateUtil.now().getTime() - 15 * 1000);
        when(timeProviderMock.now())
                .thenReturn(beforeSomeTime);
        device.setNormal(true);
        String token = createToken(device);
        this.mvc.perform(get("/auth/refresh").header("Authorization", "Bearer " + token))
                .andExpect(content().json("{access_token:null,expires_in:null}"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void shouldRefreshExpiredMobileToken() throws Exception {
        Date beforeSomeTime = new Date(DateUtil.now().getTime() - 15 * 1000);
        when(timeProviderMock.now())
                .thenReturn(beforeSomeTime);
        device.setNormal(true);
        String token = createToken(device);
        this.mvc.perform(get("/auth/refresh").header("Authorization", "Bearer " + token))
                .andExpect(content().json("{access_token:null,expires_in:null}"));
    }

    private String createToken(Device device) {
        return tokenHelper.generateToken(TEST_USERNAME, device);
    }
}
