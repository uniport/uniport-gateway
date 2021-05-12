package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import java.util.regex.Pattern;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.RoutingContext;

/**
 * Redirecting the client to a different location using regex matching and replacement.
 */
public class RedirectRegexMiddleware implements Middleware {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectRegexMiddleware.class);

  private final Pattern pattern;
  private final String replacement;

  public RedirectRegexMiddleware(String regex, String replacement) {
    this.pattern = Pattern.compile(regex);
    this.replacement = replacement;
  }

  @Override
  public void handle(RoutingContext ctx) {
    String oldURI = ctx.request().uri();

    // If the Regexp doesn't match, skip to the next handler.
    if (!this.pattern.matcher(oldURI).matches()) {
      LOGGER.debug("handle: Skipping redirect of non maching URI '{}'", oldURI);
      ctx.next();
      return;
    }

    String newURI = this.pattern.matcher(oldURI).replaceAll(this.replacement);

    LOGGER.debug("handle: Redirecting from '{}' to '{}'", oldURI, newURI);
    ctx.redirect(newURI);
  }
}
