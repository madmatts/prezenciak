package pl.wishka.common.security;

/**
 * Implemented by every security principal Wishka puts into a session (password login and Google login), so feature
 * modules can read the current user id without knowing how the user logged in.
 */
public interface AuthenticatedUser {

    long userId();
}
