package com.movie.movieticket.controller;

import com.movie.movieticket.model.User;
import com.movie.movieticket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users-admin";
    }

    @GetMapping("/search")
    public String searchUsers(@RequestParam("keyword") String keyword, Model model) {
        List<User> users = userService.findUsersByKeyword(keyword);
        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        return "users-admin";
    }

    @GetMapping("/view/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        if (user == null) {
            return "redirect:/admin/users";
        }
        model.addAttribute("user", user);
        return "view-user";
    }

    @GetMapping("/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        if (user == null) {
            return "redirect:/admin/users";
        }
        model.addAttribute("user", user);
        return "edit-user";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user, RedirectAttributes redirectAttributes) {
        userService.updateUser(id, user);
        redirectAttributes.addFlashAttribute("success", "User updated successfully!");
        return "redirect:/admin/users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
        return "redirect:/admin/users";
    }

    @GetMapping("/enable/{id}")
    public String enableUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.getUserById(id);
        if (user != null) {
            user.setEnabled(true);
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success", "User enabled successfully!");
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/disable/{id}")
    public String disableUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.getUserById(id);
        if (user != null) {
            user.setEnabled(false);
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success", "User disabled successfully!");
        }
        return "redirect:/admin/users";
    }
}
