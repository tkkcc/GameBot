use gamebot::{api::img, find::Find};

fn collect_mail() -> Option<()> {
    let mail_btn = img("asset/mail.jpg").within((0, 0, 100, 100));
    mail_btn.appear(2);
    mail_btn.find()?.click();
    Some(())
}
