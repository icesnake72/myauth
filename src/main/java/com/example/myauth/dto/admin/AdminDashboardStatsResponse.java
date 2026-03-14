package com.example.myauth.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {
  private long totalUsers;
  private long activeUsers;
  private long suspendedUsers;
  private long totalPosts;
  private long totalComments;
  private long totalDmRooms;
  private long todayNewUsers;
  private long todayNewPosts;
  private long weeklyNewUsers;
  private long weeklyNewPosts;
}
