import com.grailsrocks.emailconfirmation.EmailConfirmationService
import griffon.portal.Profile
import griffon.portal.auth.Membership
import griffon.portal.auth.User
import griffon.portal.util.MD5
import org.codehaus.groovy.grails.commons.GrailsApplication

class BootStrap {
    EmailConfirmationService emailConfirmationService
    GrailsApplication grailsApplication

    def init = { servletContext ->
        grailsApplication.config.serverURL = grailsApplication.config.grails.serverURL ?: 'http://localhost:8080/' + grailsApplication.metadata.'app.name'

        User user = new User(
                fullName: 'Andres Almiray',
                email: 'aalmiray@yahoo.com',
                username: 'aalmiray',
                password: MD5.encode('foo'),
                membership: new Membership(
                        status: Membership.Status.ADMIN,
                        reason: 'lorem impsum lorem impsum lorem impsum lorem impsum lorem impsum'
                )
        )
        user.profile = new Profile(
                user: user,
                gravatarEmail: user.email,
                website: 'http://jroller.com/aalmiray',
                twitter: 'aalmiray'
        )
        user.save()

        user = new User(
                fullName: 'Alexander Klein',
                email: 'info@aklein.org',
                username: 'saschaklein',
                password: MD5.encode('foo'),
                membership: new Membership(
                        status: Membership.Status.ACCEPTED,
                        reason: 'lorem impsum lorem impsum lorem impsum lorem impsum lorem impsum'
                )
        )
        user.profile = new Profile(
                user: user,
                gravatarEmail: user.email,
                website: '',
                twitter: 'saschaklein'
        )
        user.save()

        new User(
                fullName: 'Griffon',
                email: 'aalmiray@gmail.com',
                username: 'griffon',
                password: MD5.encode('foo'),
                membership: new Membership(
                        status: Membership.Status.PENDING,
                        reason: 'lorem impsum lorem impsum lorem impsum lorem impsum lorem impsum'
                )
        ).save()

        setupEmailConfirmationService()
    }

    private void setupEmailConfirmationService() {
        emailConfirmationService.onConfirmation = { String email, String uid ->
            User user = User.findByEmail(email)
            String nuid = MD5.encode(email)
            if (nuid == uid) {
                user.membership.status = Membership.Status.ACCEPTED
                user.profile = new Profile(user: user)
                user.profile.gravatarEmail = user.email
                user.save()
                log.info("User with id $uid has confirmed their email address $email")
                return [controller: "profile", action:  "show", id: user.username]
            }

            return {
                flash.message = "We could not confirm email (${email}) as valid."
                redirect(controller: 'user', action: 'signup')
            }
        }

        emailConfirmationService.onInvalid = { String token ->
            //log.warn("User with id $uid failed to confirm email address after 30 days")
            log.warn("Someone tried to confirm email with an invalid token: $token")
        }

        emailConfirmationService.onTimeout = { String email, String uid ->
            log.warn("User with id $uid failed to confirm email address (${email}) after 30 days")
            User.findByEmail(email)?.delete()
        }
    }

    def destroy = {
    }
}
