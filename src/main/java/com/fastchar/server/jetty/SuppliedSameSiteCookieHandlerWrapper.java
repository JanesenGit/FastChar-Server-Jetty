package com.fastchar.server.jetty;

import com.fastchar.server.SameSite;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

public class SuppliedSameSiteCookieHandlerWrapper extends HandlerWrapper {

	private final FastJettyConfig jettyConfig;

	public SuppliedSameSiteCookieHandlerWrapper(FastJettyConfig jettyConfig) {
		this.jettyConfig = jettyConfig;
	}

	@Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        HttpServletResponse wrappedResponse = new ResponseWrapper(response);
        super.handle(target, baseRequest, request, wrappedResponse);
    }

	class ResponseWrapper extends HttpServletResponseWrapper {
		ResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		@SuppressWarnings("removal")
		public void addCookie(Cookie cookie) {
			SameSite sameSite = jettyConfig.getCookieSameSite();
			if (sameSite != null) {
				String comment = HttpCookie.getCommentWithoutAttributes(cookie.getComment());
				String sameSiteComment = getSameSiteComment(sameSite);
				cookie.setComment((comment != null) ? comment + sameSiteComment : sameSiteComment);
			}
			super.addCookie(cookie);
		}

		private String getSameSiteComment(SameSite sameSite) {
			switch (sameSite) {
				case NONE:
					return HttpCookie.SAME_SITE_NONE_COMMENT;
				case LAX:
					return HttpCookie.SAME_SITE_LAX_COMMENT;
				case STRICT:
					return HttpCookie.SAME_SITE_STRICT_COMMENT;
			}
			return "";
		}

	}

}