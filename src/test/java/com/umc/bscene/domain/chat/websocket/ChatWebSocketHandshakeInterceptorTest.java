package com.umc.bscene.domain.chat.websocket;

import com.umc.bscene.domain.chat.service.ChatWebSocketTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandshakeInterceptorTest {
    private static final String TICKET = "ticket-value";

    @Mock
    private ChatWebSocketTicketService ticketService;
    @Mock
    private WebSocketHandler webSocketHandler;

    private ChatWebSocketHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ChatWebSocketHandshakeInterceptor(ticketService);
    }

    @Test
    void acceptsValidTicketAndDmSubprotocol() {
        HandshakeFixture fixture = fixture(true);
        when(ticketService.consume(TICKET)).thenReturn(1L);

        boolean accepted = interceptor.beforeHandshake(
                fixture.request(), fixture.response(), webSocketHandler, fixture.attributes());

        assertTrue(accepted);
        assertEquals(1L, fixture.attributes().get(
                ChatWebSocketHandshakeInterceptor.USER_ID_ATTRIBUTE));
    }

    @Test
    void rejectsExpiredOrInvalidTicketWithUnauthorized() {
        HandshakeFixture fixture = fixture(true);
        when(ticketService.consume(TICKET)).thenReturn(null);

        boolean accepted = interceptor.beforeHandshake(
                fixture.request(), fixture.response(), webSocketHandler, fixture.attributes());

        assertFalse(accepted);
        assertEquals(401, fixture.servletResponse().getStatus());
    }

    @Test
    void rejectsRequestWithoutDmSubprotocolWithBadRequest() {
        HandshakeFixture fixture = fixture(false);

        boolean accepted = interceptor.beforeHandshake(
                fixture.request(), fixture.response(), webSocketHandler, fixture.attributes());

        assertFalse(accepted);
        assertEquals(400, fixture.servletResponse().getStatus());
        verifyNoInteractions(ticketService);
    }

    private HandshakeFixture fixture(boolean includeSubprotocol) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setParameter("ticket", TICKET);
        if (includeSubprotocol) {
            servletRequest.addHeader("Sec-WebSocket-Protocol", "dm.v1");
        }
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        return new HandshakeFixture(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                servletResponse,
                new HashMap<>()
        );
    }

    private record HandshakeFixture(
            ServletServerHttpRequest request,
            ServletServerHttpResponse response,
            MockHttpServletResponse servletResponse,
            Map<String, Object> attributes
    ) {
    }
}
