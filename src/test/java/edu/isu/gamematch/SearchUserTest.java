package edu.isu.gamematch;


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SearchUserTest {


    @Autowired
    private SQLHandler sqlHandler;


    // ==================================================
    // HELPER METHODS
    // ==================================================


    private User createUser(long steamID, String personaName) {
        UserProfile profile = new UserProfile(personaName, null);
        User user = new User();
        user.setSteamID(steamID);
        user.setPersonaName(personaName);
        user.setUserProfile(profile);
        profile.setUser(user);
        return user;
    }


    // ==================================================
    // TEST 1: Search for a user that exists in the database
    // ==================================================
    @Test
    public void testSearchUser_returnsCorrectUser_whenProfileExists() {
        System.out.println("=== STARTING SEARCH USER TEST - USER EXISTS ===");


        // STEP 1: CREATE AND SAVE USER
        System.out.println("\n--- Step 1: Creating user ---");
        User user = createUser(111222333L, "SearchableAlice");
        assertTrue(sqlHandler.createUser(user), "User creation should succeed");
        System.out.println(" Created user: SearchableAlice");


        // STEP 2: SEARCH FOR THE USER BY PROFILE NAME
        System.out.println("\n--- Step 2: Searching for user by profile name ---");
        User result = sqlHandler.searchUser("SearchableAlice");


        // STEP 3: VERIFY RESULT
        System.out.println("\n--- Step 3: Verifying result ---");
        assertNotNull(result, "Search result should not be null");
        assertEquals("SearchableAlice", result.getPersonaName(), "Persona name should match");
        assertEquals(111222333L, result.getSteamID(), "Steam ID should match");
        System.out.println(" Found user: " + result.getPersonaName());


        System.out.println("\n=== SEARCH USER TEST - USER EXISTS COMPLETED SUCCESSFULLY ===");
    }


    // ==================================================
    // TEST 2: Search for a user that does NOT exist
    // ==================================================
    @Test
    public void testSearchUser_returnsNull_whenProfileDoesNotExist() {
        System.out.println("=== STARTING SEARCH USER TEST - USER NOT FOUND ===");


        // STEP 1: SEARCH FOR A NON-EXISTENT USER
        System.out.println("\n--- Step 1: Searching for non-existent user ---");
        User result = sqlHandler.searchUser("GhostUser_XYZ_99999");


        // STEP 2: VERIFY NULL IS RETURNED
        System.out.println("\n--- Step 2: Verifying null result ---");
        assertNull(result, "Search result should be null for a non-existent profile name");
        System.out.println(" Correctly returned null for non-existent user");


        System.out.println("\n=== SEARCH USER TEST - USER NOT FOUND COMPLETED SUCCESSFULLY ===");
    }


    // ==================================================
    // TEST 3: Search returns the correct user when multiple users exist
    // ==================================================
    @Test
    public void testSearchUser_returnsCorrectUser_whenMultipleUsersExist() {
        System.out.println("=== STARTING SEARCH USER TEST - MULTIPLE USERS ===");


        // STEP 1: CREATE MULTIPLE USERS
        System.out.println("\n--- Step 1: Creating multiple users ---");
        User user1 = createUser(111111111L, "PlayerOne");
        User user2 = createUser(222222222L, "PlayerTwo");
        User user3 = createUser(333333333L, "PlayerThree");


        assertTrue(sqlHandler.createUser(user1), "User1 creation should succeed");
        assertTrue(sqlHandler.createUser(user2), "User2 creation should succeed");
        assertTrue(sqlHandler.createUser(user3), "User3 creation should succeed");
        System.out.println(" Created 3 users: PlayerOne, PlayerTwo, PlayerThree");


        // STEP 2: SEARCH FOR A SPECIFIC USER
        System.out.println("\n--- Step 2: Searching for PlayerTwo specifically ---");
        User result = sqlHandler.searchUser("PlayerTwo");


        // STEP 3: VERIFY CORRECT USER IS RETURNED
        System.out.println("\n--- Step 3: Verifying correct user returned ---");
        assertNotNull(result, "Search result should not be null");
        assertEquals("PlayerTwo", result.getPersonaName(), "Should return PlayerTwo, not another user");
        assertEquals(222222222L, result.getSteamID(), "Steam ID should match PlayerTwo specifically");
        System.out.println(" Correctly identified PlayerTwo among multiple users");


        System.out.println("\n=== SEARCH USER TEST - MULTIPLE USERS COMPLETED SUCCESSFULLY ===");
    }


    // ==================================================
    // TEST 4: Search after updating a user's profile name
    // ==================================================
    @Test
    public void testSearchUser_returnsNull_afterProfileNameChanged() {
        System.out.println("=== STARTING SEARCH USER TEST - AFTER PROFILE UPDATE ===");


        // STEP 1: CREATE USER
        System.out.println("\n--- Step 1: Creating user ---");
        User user = createUser(444444444L, "OldUsername");
        assertTrue(sqlHandler.createUser(user), "User creation should succeed");
        System.out.println(" Created user: OldUsername");


        // STEP 2: VERIFY USER CAN BE FOUND UNDER OLD NAME
        System.out.println("\n--- Step 2: Confirming user exists under old name ---");
        User foundOld = sqlHandler.searchUser("OldUsername");
        assertNotNull(foundOld, "User should be findable under old profile name");
        System.out.println(" Confirmed user found under OldUsername");


        // STEP 3: UPDATE PROFILE NAME
        System.out.println("\n--- Step 3: Updating profile name ---");
        user.setPersonaName("NewUsername");
        user.getUserProfile().setProfileName("NewUsername");
        assertTrue(sqlHandler.updateUser(user), "User update should succeed");
        System.out.println(" Updated profile name to: NewUsername");


        // STEP 4: SEARCH OLD NAME - SHOULD RETURN NULL
        System.out.println("\n--- Step 4: Searching old name after update ---");
        User foundOldAfterUpdate = sqlHandler.searchUser("OldUsername");
        assertNull(foundOldAfterUpdate, "Old profile name should no longer return a result");
        System.out.println(" Old name correctly returns null after update");


        // STEP 5: SEARCH NEW NAME - SHOULD RETURN USER
        System.out.println("\n--- Step 5: Searching new name after update ---");
        User foundNew = sqlHandler.searchUser("NewUsername");
        assertNotNull(foundNew, "User should be findable under new profile name");
        assertEquals("NewUsername", foundNew.getPersonaName(), "Returned user should have updated name");
        System.out.println(" New name correctly returns updated user");


        System.out.println("\n=== SEARCH USER TEST - AFTER PROFILE UPDATE COMPLETED SUCCESSFULLY ===");
    }


    // ==================================================
    // TEST 5: Search after deleting a user
    // ==================================================
    @Test
    public void testSearchUser_returnsNull_afterUserDeleted() {
        System.out.println("=== STARTING SEARCH USER TEST - AFTER DELETION ===");


        // STEP 1: CREATE USER
        System.out.println("\n--- Step 1: Creating user ---");
        User user = createUser(555555555L, "TemporaryUser");
        assertTrue(sqlHandler.createUser(user), "User creation should succeed");
        System.out.println(" Created user: TemporaryUser");


        // STEP 2: VERIFY USER EXISTS
        System.out.println("\n--- Step 2: Confirming user exists ---");
        User found = sqlHandler.searchUser("TemporaryUser");
        assertNotNull(found, "User should exist before deletion");
        System.out.println(" Confirmed user exists");


        // STEP 3: DELETE USER
        System.out.println("\n--- Step 3: Deleting user ---");
        assertTrue(sqlHandler.deleteUser(user), "User deletion should succeed");
        System.out.println(" Deleted user: TemporaryUser");


        // STEP 4: SEARCH DELETED USER - SHOULD RETURN NULL
        System.out.println("\n--- Step 4: Searching for deleted user ---");
        User foundAfterDelete = sqlHandler.searchUser("TemporaryUser");
        assertNull(foundAfterDelete, "Deleted user should not be returned by search");
        System.out.println(" Correctly returned null for deleted user");


        System.out.println("\n=== SEARCH USER TEST - AFTER DELETION COMPLETED SUCCESSFULLY ===");
    }
}




