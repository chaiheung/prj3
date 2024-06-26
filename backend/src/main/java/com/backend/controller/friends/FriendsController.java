package com.backend.controller.friends;

import com.backend.domain.friends.FriendDto;
import com.backend.domain.friends.FriendRequest;
import com.backend.service.friends.FriendsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendsController {

	private final FriendsService friendsService;
	private static final Logger logger = LoggerFactory.getLogger(FriendsController.class);

	public FriendsController(FriendsService friendsService) {
		this.friendsService = friendsService;
	}

	@GetMapping("/{memberId}")
	public List<FriendDto> getFriends(@PathVariable Integer memberId) {
		logger.info("Received request to fetch friends for member ID: {}", memberId);
		List<FriendDto> friends = friendsService.getFriendsWithIds(memberId);
		logger.info("Returning friends for member ID {}: {}", memberId, friends);
		return friends;
	}

	@PostMapping("/add")
	public ResponseEntity<String> addFriend(@RequestBody FriendRequest friendRequest) {
		logger.info("Received request to add friend: {}", friendRequest);

		try {
			friendsService.addFriend(friendRequest);
			return ResponseEntity.ok("Friend added successfully");
		} catch (Exception e) {
			logger.error("Error adding friend", e);
			return ResponseEntity.status(500).body("Error adding friend");
		}
	}

	@GetMapping("/check")
	public ResponseEntity<Boolean> checkFriendship(@RequestParam Integer memberId, @RequestParam Integer friendId) {
		boolean isFriend = friendsService.checkFriendship(memberId, friendId);
		return ResponseEntity.ok(isFriend);
	}

	@DeleteMapping("/delete")
	public ResponseEntity<String> deleteFriend(@RequestParam Integer memberId, @RequestParam Integer friendId) {
		logger.info("Received request to delete friend. Member ID: {}, Friend ID: {}", memberId, friendId);
		try {
			friendsService.deleteFriend(memberId, friendId);
			return ResponseEntity.ok("Friend deleted successfully");
		} catch (Exception e) {
			logger.error("Error deleting friend", e);
			return ResponseEntity.status(500).body("Error deleting friend");
		}
	}
}
