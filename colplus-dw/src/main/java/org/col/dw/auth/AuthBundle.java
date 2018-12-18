package org.col.dw.auth;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.col.api.model.ColUser;
import org.col.dw.PgAppConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class AuthBundle implements ConfiguredBundle<PgAppConfig> {
  
  private JwtCodec jwtCodec;
  private IdentityService idService;
  
  @Override
  public void run(PgAppConfig cfg, Environment environment) {
    environment.jersey().register(RolesAllowedDynamicFeature.class);
    
    jwtCodec = new JwtCodec(cfg.jwtKey);
    idService = new IdentityService(cfg.auth.createAuthenticationProvider(), cfg.authCache);
    
    environment.jersey().register(new AuthDynamicFeature(new AuthFilter(idService, jwtCodec)));
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(ColUser.class));
  }
  
  @Override
  public void initialize(Bootstrap<?> bootstrap) {
  
  }
  
  public IdentityService getIdentityService() {
    return idService;
  }
  
  public JwtCodec getJwtCodec() {
    return jwtCodec;
  }
}
