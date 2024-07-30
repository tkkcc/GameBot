use serde::{Deserialize, Serialize};

#[typetag::serde(tag = "type")]
trait Component {}

#[derive(Serialize, Deserialize, Default)]
struct Column {
    children: Vec<Box<dyn Component>>,
}

#[typetag::serde]
impl Component for Column {}

#[derive(Serialize, Deserialize, Default)]
struct TextField {
    text: String,
    id: String,
}

#[derive(Serialize, Deserialize)]
struct Button {
    text: String,
    id: String,
}

#[derive(Serialize, Deserialize)]
struct Text {
    text: String,
}

#[typetag::serde]
impl Component for TextField {}

#[typetag::serde]
impl Component for Text {}

#[typetag::serde]
impl Component for Button {}

fn t() {
    #[derive(Default)]
    struct Account {
        username: String,
        password: String,
    }
    #[derive(Default)]
    struct State {
        account: Vec<Account>,
    }
    let state = State::default();

    let layout = Column {
        children: state
            .account
            .iter()
            .enumerate()
            .flat_map(|(i, account)| {
                [
                    Box::new(TextField {
                        text: "user".into(),
                        id: "username".into(),
                    }),
                    Box::new(Button {
                        text: format!("ok${i}"),
                        id: format!("ok${i}"),
                    }) as Box<dyn Component>,
                ]
            })
            .collect(),
    };

    let mut account = Account::default();
    let a = &mut account.username;
    let b = &mut account.password;
    dbg!(&a);
    dbg!(&b);
    dbg!(&a);
    dbg!(&b);
    let c = account.username;
    dbg!(&b);

    enum E1 {
        Abc(u8),
    }
    fn f(x: impl Fn(u8) -> E1) {}
    f(E1::Abc);

    // fn f(x: impl Into<impl Iterator>) {
    //     // let iter: Iterator = x.into();
    // }

    //
    // div().flex().flex_col().size_full().justify_around().child(
    //     div().flex().flex_row().w_full().justify_around().child(
    //         div()
    //             .flex()
    //             .bg(rgb(0x2e7d32))
    //             .size(Length::Definite(Pixels(300.0).into()))
    //             .justify_center()
    //             .items_center()
    //             .shadow_lg()
    //             .text_xl()
    //             .text_color(black())
    //             .child("hello")
    //             .child(
    //                 svg()
    //                     .size_8()
    //                     .path("examples/image/arrow_circle.svg")
    //                     .text_color(black())
    //                     .with_animation(
    //                         "image_circle",
    //                         Animation::new(Duration::from_secs(2))
    //                             .repeat()
    //                             .with_easing(bounce(ease_in_out)),
    //                         |svg, delta| {
    //                             svg.with_transformation(Transformation::rotate(percentage(delta)))
    //                         },
    //                     ),
    //             ),
    //     ),
    // );
    // col((row(())));

    // fn app_logic(data: &mut u32) -> impl View<u32, (), Element = impl Widget> {
    //     Column::new((
    //         Button::new(format!("count: {}", data), |data| *data += 1),
    //         Button::new("reset", |data| *data = 0),
    //     ))
    // }
}

#[cfg(test)]
mod test {

    use crate::t::Column;

    #[test]
    fn test() {
        let x = Column { children: vec![] };
        dbg!(serde_json::to_string(&x).unwrap());

        assert_eq!(1, 1);
    }
}
