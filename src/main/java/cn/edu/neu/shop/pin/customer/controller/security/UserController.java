package cn.edu.neu.shop.pin.customer.controller.security;

import cn.edu.neu.shop.pin.customer.service.security.UserService;
import cn.edu.neu.shop.pin.dto.UserDataDTO;
import cn.edu.neu.shop.pin.model.PinUser;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ydy
 */

@RestController
@RequestMapping("/user")
@Api(tags = "user")
public class UserController {

    @Autowired
    private UserService userService;

    //这里在登录，得到token
    @GetMapping("/sign-in")
    @ApiOperation(value = "${UserController.signIn}")
    @ApiResponses(value = {//
            @ApiResponse(code = 400, message = "Something went wrong"), //
            @ApiResponse(code = 422, message = "Invalid id/password supplied")})
    public String signIn(//
                         @ApiParam("Id") @RequestParam String id, //
                         @ApiParam("Password") @RequestParam String password) {
        return userService.signIn(id, password);
    }

    //这里在注册，保存信息，并且得到token
    @PostMapping("/sign-up")
    @ApiOperation(value = "${UserController.signUp}")
    @ApiResponses(value = {//
            @ApiResponse(code = 400, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 422, message = "Id is already in use"), //
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public String signUp(@ApiParam("Sign-up User") @RequestBody UserDataDTO user) {
//    PinUser pinUser = new PinUser(user.getId(),user.getPassword(),user.getRoles());
//    return userService.signUp(pinUser);
        return null;
    }

    //
    @DeleteMapping(value = "/delete/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiOperation(value = "${UserController.delete}")
    @ApiResponses(value = {//
            @ApiResponse(code = 400, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "The user doesn't exist"), //
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public String delete(@ApiParam("Id") @PathVariable String id) {
        System.out.println("123");
        userService.delete(id);
        return id;
    }

    @GetMapping(value = "/get")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ApiOperation(value = "${UserController.search}", response = PinUser.class)
    @ApiResponses(value = {//
            @ApiResponse(code = 400, message = "Something went wrong"), //
            @ApiResponse(code = 403, message = "Access denied"), //
            @ApiResponse(code = 404, message = "The user doesn't exist"), //
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public PinUser search(@ApiParam("Id") @RequestParam String id) {
        return userService.search(id);
    }

    @GetMapping(value = "/me")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER') or hasRole('ROLE_DEALER')")
    @ApiOperation(value = "${UserController.me}", response = PinUser.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Something went wrong"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 500, message = "Expired or invalid JWT token")})
    public /*UserResponseDTO*/PinUser whoAmI(HttpServletRequest req) {
//    return modelMapper.map(userService.whoAmI(req), UserResponseDTO.class);
        return userService.whoAmI(req);
    }

    @GetMapping("/refresh")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER')")
    public String refresh(HttpServletRequest req) {
        return userService.refresh(req.getRemoteUser());
    }

}
