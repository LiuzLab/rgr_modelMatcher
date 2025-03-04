package ubc.pavlab.rdp.controllers;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.Profile;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.services.PrivacyService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ubc.pavlab.rdp.util.TestUtils.createUser;

@RunWith(SpringRunner.class)
@WebMvcTest(LoginController.class)
public class LoginControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PrivacyService privacyService;

    @MockBean
    private ApplicationSettings applicationSettings;

    @MockBean
    private ApplicationSettings.PrivacySettings privacySettings;

    @MockBean(name = "siteSettings")
    private SiteSettings siteSettings;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private FormattingConversionService formattingConversionService;

    @MockBean(name = "ontologyMessageSource")
    private MessageSource ontologyMessageSource;

    @Before
    public void setUp() {
        when( applicationSettings.getPrivacy() ).thenReturn( privacySettings );
        when( siteSettings.getContactEmail() ).thenReturn( "support@example.com" );
        when( privacyService.getDefaultPrivacyLevel() ).thenReturn( PrivacyLevelType.PRIVATE );
    }

    @Test
    public void login_thenReturnSuccess() throws Exception {
        mvc.perform( get( "/login" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "login" ) )
                .andExpect( content().contentTypeCompatibleWith( MediaType.TEXT_HTML ) )
                .andExpect( content().string( containsString( "support@example.com" ) ) );
    }

    @Test
    @WithMockUser
    public void login_whenLoggedIn_thenForward() throws Exception {
        mvc.perform( get( "/login" ) )
                .andExpect( status().isOk() )
                .andExpect( forwardedUrl( "/" ) );
    }

    @Test
    @WithMockUser
    public void login_whenAlreadyLoggedIn_thenRedirect3xx() throws Exception {
        mvc.perform( get( "/login" ) )
                .andExpect( status().isOk() )
                .andExpect( forwardedUrl( "/" ) );
    }

    @Test
    public void register_thenReturnSuccess() throws Exception {
        mvc.perform( get( "/registration" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "registration" ) )
                .andExpect( model().attribute( "user", new User() ) );

        when( applicationSettings.getPrivacy() ).thenReturn( privacySettings );
        when( userService.create( any() ) ).thenAnswer( answer -> answer.getArgument( 0, User.class ) );
    }

    @Test
    @Ignore("I have absolutely no idea why this converter does not work anymore. See https://github.com/PavlidisLab/rdp/issues/171 for details.")
    public void register_whenEmailIsUsedButNotEnabled_thenResendConfirmation() throws Exception {
        User user = User.builder( new Profile() )
                .email( "foo@example.com" )
                .enabled( false )
                .build();
        when( userService.findUserByEmailNoAuth( "foo@example.com" ) ).thenReturn( user );

        //noinspection Convert2Lambda
        formattingConversionService.addConverter( new Converter<Object, User>() {
            @Override
            public User convert( Object o ) {
                return User.builder( Profile.builder().name( "Foo" ).build() )
                        .email( "foo@example.com" )
                        .build();
            }
        } );

        mvc.perform( post( "/registration" )
                        .locale( Locale.getDefault() )
                        .param( "user.profile.name", "Foo" )
                        .param( "user.email", "foo@example.com" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "registration" ) )
                .andExpect( model().attribute( "user", new User() ) );

        verify( userService ).createVerificationTokenForUser( user, Locale.getDefault() );
    }

    @Test
    public void resendConfirmation_thenReturnSuccess() throws Exception {
        mvc.perform( get( "/resendConfirmation" )
                        .locale( Locale.getDefault() ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "resendConfirmation" ) );

        User user = createUser( 1 );
        user.setEnabled( false );
        when( userService.findUserByEmailNoAuth( "foo@example.com" ) ).thenReturn( user );

        mvc.perform( post( "/resendConfirmation" )
                        .locale( Locale.getDefault() )
                        .param( "email", "foo@example.com" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "resendConfirmation" ) );

        verify( userService ).findUserByEmailNoAuth( "foo@example.com" );
        verify( userService ).createVerificationTokenForUser( user, Locale.getDefault() );
    }

    @Test
    public void registrationConfirm_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.confirmVerificationToken( "1234" ) ).thenReturn( user );
        mvc.perform( get( "/registrationConfirm" )
                        .param( "token", "1234" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/login" ) )
                .andExpect( flash().attributeExists( "message" ) );
    }

    @Test
    public void registrationConfirm_whenTokenDoesNotExist_thenReturnError() throws Exception {
        when( userService.confirmVerificationToken( "1234" ) ).thenThrow( TokenException.class );
        mvc.perform( get( "/registrationConfirm" )
                        .param( "token", "1234" ) )
                .andExpect( status().isNotFound() )
                .andExpect( view().name( "error/404" ) );
    }
}
