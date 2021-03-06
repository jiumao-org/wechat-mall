package org.jiumao.mall.OAuth2;

import java.util.HashMap;

import io.netty.channel.ChannelHandlerContext;

import org.apache.commons.lang3.StringUtils;
import org.jiumao.common.AsynHttp.AsynHttpClient;
import org.jiumao.common.AsynHttp.AsynHttps;
import org.jiumao.common.utils.JsonUtil;
import org.jiumao.mall.OAuth2.client.ClientToken;
import org.jiumao.mall.OAuth2.code.CodeToken;
import org.jiumao.mall.OAuth2.code.ReturnCode;
import org.jiumao.mall.OAuth2.domain.Authorize;
import org.jiumao.mall.OAuth2.domain.GrantType;
import org.jiumao.mall.OAuth2.domain.ResponseType;
import org.jiumao.mall.OAuth2.implicit.ImplicitToken;
import org.jiumao.mall.OAuth2.password.PasswdToken;
import org.jiumao.mall.auth.Auths;
import org.jiumao.mall.domain.Authentication;
import org.jiumao.remote.common.NettyHandler;
import org.jiumao.remote.service.RemotingCommand;


public class OAuth2ServerHandler extends NettyHandler {

    public void sendRedirect(Authorize auth, String... params) {
        if (StringUtils.isEmpty(auth.getRedirect_uri())) {
            return;
        }
        StringBuilder url = new StringBuilder(64);
        url.append(auth.getRedirect_uri()).append(AsynHttps.QUESTION_MARK);
        for (String p : params) {
            url.append(p).append(AsynHttps.AND);
        }
        url.append("time=").append(System.currentTimeMillis());
        if (!StringUtils.isEmpty(auth.getState())) {
            url.append(AsynHttps.AND).append("state=").append(auth.getState());
        }
        AsynHttpClient.GET(url.toString(), AsynHttps.RESPONCE_302);
    }


    @Override
    public void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg) {
        HashMap<String, String> map = msg.getExtFields();
        String key = map.get("action");
        switch (key) {
            case "authorize":
                auth(ctx, msg);
                break;

            case "token":
                token(ctx, msg);
                break;

            case "access":
                access(ctx, msg);
                break;

            default:
                break;
        }
        ctx.write(msg);
    }


    private void access(ChannelHandlerContext ctx, RemotingCommand msg) {
        String token = msg.getExtFields().get("token");
        Boolean sign = OAuth2Provider.sign(token);
        msg.getExtFields().put("access", sign.toString());
    }


    private void token(ChannelHandlerContext ctx, RemotingCommand msg) {
        byte[] entity = null;
        Authentication auth = JsonUtil.decode(msg.getBody(), Authentication.class);
        GrantType type = GrantType.valueOf(auth.getGrant_type());
        switch (type) {
            case authorization_code:
                CodeToken ct = OAuth2Provider.hasCode(auth.getCode());
                if (null != ct) {
                    entity = JsonUtil.toBytes(ct);
                }
                break;
            case password:
                // base64 author
                PasswdToken pwd = OAuth2Provider.login(auth.getUsername(), auth.getPassword());
                if (null != pwd) {
                    entity = JsonUtil.toBytes(pwd);
                }
                break;
            case clientcredentials:
                ClientToken clientToken = OAuth2Provider.client(auth.getClient_id(), auth.getClient_secret());
                if (null != clientToken) {
                    entity = JsonUtil.toBytes(clientToken);
                }
                break;
            case refresh_token:
                PasswdToken pwdT = OAuth2Provider.refresh(auth.getClient_id(), auth.getClient_secret(), auth.getRefresh_token());
                if (null != pwdT) {
                    entity = JsonUtil.toBytes(pwdT);
                }
                break;

            default:
                break;
        }
        msg.setBody(entity);
    }


    private void auth(ChannelHandlerContext ctx, RemotingCommand msg) {
        Authorize auth = JsonUtil.decode(msg.getBody(), Authorize.class);

        ResponseType type = ResponseType.valueOf(auth.getResponse_type());
        byte[] entity = null;
        switch (type) {
            /*
             * HTTP/1.1 302 Found Location:
             * https://client.example.com/cb?code=SplxlOBeZQQYbYS6WxSbIA &state=xyz
             */
            case code:
                ReturnCode code = OAuth2Provider.giveCode(auth.getClient_id());
                if (null != code) {
                    entity = JsonUtil.toBytes(code);
                }

                sendRedirect(auth, "code=" + code.getCode());
                break;

            /*
             * HTTP/1.1 302 Found Location:
             * http://example.com/cb#access_token=2YotnFZFEjr1zCsicMWpAA
             * &state=xyz&token_type=example&expires_in=3600
             */
            case token:
                ImplicitToken token = OAuth2Provider.giveToken(auth.getClient_id());
                if (null != token) {
                    token.setState(auth.getState());
                    entity = JsonUtil.toBytes(token);
                }

                sendRedirect(auth, "access_token=" + token.getAccess_token(), "token_type=" + token.getToken_type(),
                        "expires_in=" + token.getExpires_in());
                break;

            default:
                break;
        }
        msg.setBody(entity);
    }
}
