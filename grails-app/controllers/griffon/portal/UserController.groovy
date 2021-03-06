/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.portal

import com.grailsrocks.emailconfirmation.EmailConfirmationService
import grails.converters.JSON
import griffon.portal.auth.Membership
import griffon.portal.auth.User
import griffon.portal.util.MD5
import groovy.text.SimpleTemplateEngine
import org.apache.commons.lang.RandomStringUtils
import org.grails.mail.MailService
import org.grails.plugin.jcaptcha.JcaptchaService
import griffon.portal.values.SettingsTab

/**
 * @author Andres Almiray
 */
class UserController {
    JcaptchaService jcaptchaService
    MailService mailService
    EmailConfirmationService emailConfirmationService

    def login(LoginCommand command) {
        if (!params.filled) {
            // renders 1st hit
            render(view: 'signin', model: [command: new LoginCommand()])
            return
        }

        if (!command.validate()) {
            // renders with errors
            render(view: 'signin', model: [command: command])
            return
        }

        User user = User.findWhere(username: command.username,
                password: MD5.encode(command.passwd))
        if (!user) {
            command.errors.rejectValue('username', 'griffon.portal.auth.User.username.nomatch.message')
            render(view: 'signin', model: [command: command, originalURI: params.orinialURI])
            return
        }

        if (!user.profile) {
            render(view: 'subscribe', model: [userInstance: user])
            return
        }
        session.user = user
        session.profile = user.profile
        if (params.originalURI) {
            redirect(uri: params.originalURI)
        } else {
            redirect(controller: "profile", action: "show", id: user.username)
        }
    }

    def logout() {
        session.user = null
        session.profile = null
        redirect(uri: '/')
    }

    def signup(SignupCommand command) {
        if (!params.filled) {
            render(view: 'signup', model: [command: new SignupCommand()])
            return
        }

        if (!command.validate()) {
            render(view: 'signup', model: [command: command])
            return
        }

        if (!jcaptchaService.validateResponse('image', session.id, command.captcha)) {
            command.errors.rejectValue('captcha', 'griffon.portal.auth.User.invalid.captcha.message')
            render(view: 'signup', model: [command: command])
            return
        }

        User user = new User()
        user.properties = command.properties
        user.membership.status = Membership.Status.PENDING
        user.password = MD5.encode(user.password)
        if (!user.save(flush: true)) {
            user.errors.fieldErrors.each { error ->
                command.errors.rejectValue(
                        error.field,
                        error.code,
                        error.arguments,
                        error.defaultMessage
                )
            }
            command.password = params.password
            render(view: 'signup', model: [command: command])
            return
        }

        emailConfirmationService.sendConfirmation(
                user.email,
                'Please confirm',
                [from: grailsApplication.config.grails.mail.default.from, user: user.username],
                MD5.encode(user.email)
        )

        render(view: 'subscribe', model: [userInstance: user])
    }

    def membership() {
        User user = User.get(params.id)
        user.membership.reason = params.reason
        if (!user.save(flush: true)) {
            render([code: 'ERROR'] as JSON)
        } else {
            if (user.membership.status != Membership.Status.PENDING) {
                user.membership.status = Membership.Status.PENDING
                user.save()
            }
            render([code: 'OK'] as JSON)
        }
    }

    def pending() {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        String query = 'from User as u where u.membership.status = :status'
        List userInstanceList = User.findAll(query, [status: Membership.Status.PENDING], params)
        [userInstanceList: userInstanceList, userInstanceTotal: userInstanceList.size()]
    }

    def approveOrReject() {
        User user = User.get(params.id)
        user.membership.status = params.status
        user.save()
        redirect(action: 'pending')
    }

    def forgot_password(ForgotPasswordCommand command) {
        if (!params.filled) {
            render(view: 'forgot_password', model: [command: new ForgotPasswordCommand()])
            return
        }

        if (!command.validate()) {
            render(view: 'forgot_password', model: [command: command])
            return
        }

        if (!jcaptchaService.validateResponse('image', session.id, command.captcha)) {
            command.errors.rejectValue('captcha', 'griffon.portal.auth.User.invalid.captcha.message')
            render(view: 'forgot_password', model: [command: command])
            return
        }

        User user = User.findByUsername(command.username)
        if (!user) {
            command.errors.rejectValue('username', 'griffon.portal.auth.User.username.notfound.message')
            render(view: 'forgot_password', model: [command: command])
            return
        }

        sendCredentials(user)
        flash.message = "Please check your email (${user.email}) for further instructions."
        command.captcha = ''
        [command: command]
    }

    def forgot_username(ForgotUsernameCommand command) {
        if (!params.filled) {
            render(view: 'forgot_username', model: [command: new ForgotUsernameCommand()])
            return
        }

        if (!command.validate()) {
            render(view: 'forgot_username', model: [command: command])
            return
        }

        if (!jcaptchaService.validateResponse('image', session.id, command.captcha)) {
            command.errors.rejectValue('captcha', 'griffon.portal.auth.User.invalid.captcha.message')
            render(view: 'forgot_username', model: [command: command])
            return
        }

        User user = User.findByEmail(command.email)
        if (!user) {
            command.errors.rejectValue('email', 'griffon.portal.auth.User.email.notfound.message')
            render(view: 'forgot_username', model: [command: command])
            return
        }

        sendCredentials(user)
        flash.message = "Please check your email (${user.email}) for further instructions."
        command.captcha = ''
        [command: command]
    }

    private void sendCredentials(User user) {
        String newPassword = RandomStringUtils.randomAlphanumeric(8)
        user.password = MD5.encode(newPassword)
        user.save()

        SimpleTemplateEngine template = new SimpleTemplateEngine()
        mailService.sendMail {
            to user.email
            subject 'Password Reset'
            html template.createTemplate(grailsApplication.config.template.forgot.credentials.toString()).make(
                    ipaddress: request.remoteAddr,
                    serverURL: grailsApplication.config.serverURL,
                    username: user.username,
                    password: newPassword
            ).toString()
        }
    }

    def list() {
        if (session.user?.membership?.status != Membership.Status.ADMIN)
            redirect(uri: '/')
        [users: User.listOrderByUsername(params), userCount: User.count()]
    }

    def show() {
        if (session.user?.membership?.status != Membership.Status.ADMIN)
            redirect(uri: '/')
        if (!params.id) {
            redirect(action: 'list')
            return
        }
        [user: User.findByUsername(params.id)]
    }

    def save() {
        if (session.user?.membership?.status != Membership.Status.ADMIN)
            redirect(uri: '/')
        if (!params.id) {
            redirect(action: 'list')
            return
        }
        User userInstance = User.findByUsername(params.id)
        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), params.id])
            redirect(action: 'list')
            return
        }
        userInstance.properties = params.properties
        userInstance.profile.notifications.watchlist = params["profile.notifications.watchlist"] != null
        userInstance.profile.notifications.content = params["profile.notifications.content"] != null
        userInstance.profile.notifications.comments = params["profile.notifications.comments"] != null

        if (!userInstance.save()) {
            return render(view: 'show', model: [user: userInstance])
        }
        flash.message = message(code: 'admin.user.save.success', args: [params.id])
        redirect(action: "show", id: userInstance.username)
    }

    def delete() {
        if (session.user?.membership?.status != Membership.Status.ADMIN)
            redirect(uri: '/')
        if (!params.id) {
            redirect(action: 'list')
            return
        }
        User userInstance = User.findByUsername(params.id)
        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), params.id])
            redirect(action: 'list')
            return
        }
        userInstance.delete()
        flash.message = message(code: 'admin.user.delete.success', args: [params.id])
        redirect(action: 'list')
    }

    def changeMembership() {
        if (session.user?.membership?.status != Membership.Status.ADMIN)
            redirect(uri: '/')
        if (!params.id) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), params.id])
            response.status = 404
            render(template: "/shared/errors_and_messages", model: [cssClass: 'span16'])
        }
        if (!params.status) {
            flash.message = message(code: 'admin.user.changeMembership.nostatus', args: [params.id])
            response.status = 400
            render(template: "/shared/errors_and_messages", model: [cssClass: 'span16'])
        }
        User userInstance = User.findByUsername(params.id)
        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), params.id])
            response.status = 404
            render(template: "/shared/errors_and_messages", model: [cssClass: 'span16'])
        }
        try {
            Membership.Status status = Membership.Status.valueOf(Membership.Status, params.status)
            userInstance.membership.status = status
            userInstance.save()
            render(template: "/user/membership", model: ['user': params.id, 'currentStatus': params.status])
        } catch (IllegalArgumentException e) {
            flash.message = message(code: 'admin.user.changeMembership.wrongstatus', args: [params.status])
            response.status = 400
            render(template: "/shared/errors_and_messages", model: [cssClass: 'span16'])
        }
    }
}

class SignupCommand {
    boolean filled
    String username
    String email
    String password
    String password2
    String captcha

    static constraints = {
        username(nullable: false, blank: false)
        email(nullable: false, blank: false, email: true)
        password(nullable: false, blank: false)
        password2(nullable: false, blank: false)
        captcha(nullable: false, blank: false)
    }
}

class LoginCommand {
    boolean filled
    String username
    String passwd

    static constraints = {
        username(nullable: false, blank: false)
        passwd(nullable: false, blank: false)
    }
}

class ForgotPasswordCommand {
    boolean filled
    String username
    String captcha

    static constraints = {
        username(nullable: false, blank: false)
        captcha(nullable: false, blank: false)
    }
}

class ForgotUsernameCommand {
    boolean filled
    String email
    String captcha

    static constraints = {
        email(nullable: false, email: true)
        captcha(nullable: false, blank: false)
    }
}