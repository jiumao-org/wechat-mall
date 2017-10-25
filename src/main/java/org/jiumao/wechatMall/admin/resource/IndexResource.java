package org.jiumao.wechatMall.admin.resource;

import javax.annotation.Resource;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jiumao.wechatMall.common.Authentication;
import org.jiumao.wechatMall.common.constant.LoggerName;
import org.jiumao.wechatMall.entity.User;
import org.jiumao.wechatMall.service.UserService;
import org.jiumao.wechatMall.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/admin/index")
public class IndexResource {
	private static final Logger log = LoggerFactory
			.getLogger(LoggerName.Server);

	@Resource
	UserService userService;

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String helloWorld(
			@QueryParam("msg") @DefaultValue("hello world") String msg) {
		return msg;
	}

	@POST
	@Path("login")
	@Produces(MediaType.APPLICATION_JSON)
	public String login(@Context Authentication auth,
			@QueryParam("userName") String userName,
			@QueryParam("password") String password,
			@QueryParam("verificationCode") String verificationCode,
			@QueryParam("phone") Integer phone) {
		// check not null
		User u = new User();// dao 应该查询
		u.setPhone(phone);
		u.setUserName(userName);
		u.setPassword(password);
		User res = userService.selectUserByObj(u);
		log.info("login:" + phone + "|" + userName);

		auth.setId("" + u.getId());
		return JsonUtil.bean2Json(u);
	}

	@POST
	@Path("register")
	@Produces(MediaType.APPLICATION_JSON)
	public String register(@Context Authentication auth,
			@QueryParam("userName") String userName,
			@QueryParam("password") String password,
			@QueryParam("phone") Integer phone,
			@QueryParam("email") String email, @QueryParam("nick") String nick,
			@QueryParam("addr") String addr, @QueryParam("gender") String gender) {
		// check not null
		User u = new User();// dao 应该查询
		u.setAddr(addr);
		u.setEmail(email);
		u.setGender(gender);
		u.setNick(nick);
		u.setPassword(password);
		u.setPhone(phone);
		Long userId = 0L;
		u.setUserId(userId);
		u.setUserName(userName);
		int res = userService.insertNonEmptyUser(u);

		auth.setId("" + u.getId());
		return JsonUtil.bean2Json(u);
	}

	@GET
	@Path("info/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public String info(@PathParam("id") String name,
			@Context Authentication auth) {
		return "{}";
	}

}
