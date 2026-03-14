package com.example.myauth.dto.admin;

import com.example.myauth.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRoleChangeRequest {

  @NotNull(message = "변경할 역할은 필수입니다")
  private User.Role role;
}
