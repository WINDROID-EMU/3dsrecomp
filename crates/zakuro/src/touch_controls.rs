//! Virtual on-screen touch gamepad controls for mobile devices (Android) and touchscreens.
//! Optimized for landscape handheld gaming with authentic 3DS layout ergonomics.

use std::collections::HashMap;

use egui::{epaint::CornerRadius, Align2, Color32, FontId, Pos2, Rect, Stroke, StrokeKind, Ui, Vec2};
use winit::event::{Touch, TouchPhase};
use zakuro_core::services::hid::{InputState, PadState};
use zakuro_gpu::{layout, ScreenLayout};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TouchTarget {
    Button(PadState),
    CirclePad,
    Menu,
    BottomScreen,
}

pub struct TouchControls {
    /// Active touches mapped by touch ID.
    active_touches: HashMap<u64, (TouchTarget, Pos2)>,
    /// Circle pad normalized offset (-1.0 to 1.0).
    circle_offset: Vec2,
    /// Stick touch ID if circle pad is currently being dragged.
    stick_touch_id: Option<u64>,
    /// Bottom screen touch coordinate in 3DS screen pixels (320x240).
    bottom_touch: Option<(u16, u16)>,
    /// Set when the user taps the on-screen Menu button.
    pub toggle_menu: bool,
}

impl Default for TouchControls {
    fn default() -> Self {
        TouchControls {
            active_touches: HashMap::new(),
            circle_offset: Vec2::ZERO,
            stick_touch_id: None,
            bottom_touch: None,
            toggle_menu: false,
        }
    }
}

impl TouchControls {
    pub fn new() -> Self {
        Self::default()
    }

    /// Releases all active touches (e.g. when losing focus or switching screens).
    pub fn release(&mut self) {
        self.active_touches.clear();
        self.circle_offset = Vec2::ZERO;
        self.stick_touch_id = None;
        self.bottom_touch = None;
    }

    /// Returns and clears any menu toggle request.
    pub fn take_menu_toggle(&mut self) -> bool {
        let toggle = self.toggle_menu;
        self.toggle_menu = false;
        toggle
    }

    /// Checks if a button is currently pressed.
    pub fn is_pressed(&self, button: PadState) -> bool {
        self.active_touches.values().any(|(target, _)| *target == TouchTarget::Button(button))
    }

    /// Overlays touch button and circle pad state onto the frame input state.
    pub fn apply(&self, mut state: InputState) -> InputState {
        for (target, _) in self.active_touches.values() {
            if let TouchTarget::Button(btn) = target {
                state.buttons.insert(*btn);
            }
        }
        if self.circle_offset != Vec2::ZERO {
            state.circle_x = self.circle_offset.x;
            state.circle_y = self.circle_offset.y;
        }
        if let Some(pos) = self.bottom_touch {
            state.touch = Some(pos);
        }
        state
    }

    /// Safe margin from screen edges on ultrawide displays (19.5:9, 20:9, 21:9).
    pub fn margin_x(screen_w: f32, screen_h: f32) -> f32 {
        if screen_h > 0.0 && screen_w / screen_h > 1.65 {
            (screen_w * 0.045).clamp(32.0, 75.0)
        } else {
            18.0
        }
    }

    /// Primary left analog thumbstick (Circle Pad). Positioned where the thumb naturally rests.
    pub fn circle_pad_center(&self, screen_w: f32, screen_h: f32) -> Pos2 {
        let mx = Self::margin_x(screen_w, screen_h);
        Pos2::new(mx + 65.0, (screen_h * 0.42).clamp(110.0, screen_h - 160.0))
    }

    /// Secondary directional D-Pad, positioned below the Circle Pad just like the 3DS hardware.
    pub fn dpad_center(&self, screen_w: f32, screen_h: f32) -> Pos2 {
        let mx = Self::margin_x(screen_w, screen_h);
        Pos2::new(mx + 65.0, (screen_h * 0.42 + 130.0).clamp(180.0, screen_h - 55.0))
    }

    /// Action buttons diamond (ABXY).
    pub fn abxy_center(&self, screen_w: f32, screen_h: f32) -> Pos2 {
        let mx = Self::margin_x(screen_w, screen_h);
        Pos2::new(screen_w - mx - 75.0, (screen_h * 0.52).clamp(120.0, screen_h - 85.0))
    }

    /// Left shoulder bumper (L).
    pub fn l_rect(&self, screen_w: f32, screen_h: f32) -> Rect {
        let mx = Self::margin_x(screen_w, screen_h);
        Rect::from_min_size(Pos2::new(mx + 8.0, 14.0), Vec2::new(76.0, 40.0))
    }

    /// Right shoulder bumper (R).
    pub fn r_rect(&self, screen_w: f32, screen_h: f32) -> Rect {
        let mx = Self::margin_x(screen_w, screen_h);
        Rect::from_min_size(Pos2::new(screen_w - mx - 84.0, 14.0), Vec2::new(76.0, 40.0))
    }

    /// Quick in-game menu button (centered top).
    pub fn menu_rect(&self, screen_w: f32, _screen_h: f32) -> Rect {
        Rect::from_min_size(Pos2::new(screen_w * 0.5 - 32.0, 12.0), Vec2::new(64.0, 28.0))
    }

    /// Select button (bottom center-left).
    pub fn select_rect(&self, screen_w: f32, screen_h: f32) -> Rect {
        Rect::from_min_size(Pos2::new(screen_w * 0.5 - 76.0, screen_h - 32.0), Vec2::new(68.0, 26.0))
    }

    /// Start button (bottom center-right).
    pub fn start_rect(&self, screen_w: f32, screen_h: f32) -> Rect {
        Rect::from_min_size(Pos2::new(screen_w * 0.5 + 8.0, screen_h - 32.0), Vec2::new(68.0, 26.0))
    }

    /// Processes a winit touch event.
    pub fn handle_touch(
        &mut self,
        touch: &Touch,
        window_size: (u32, u32),
        scale_factor: f32,
        arrangement: ScreenLayout,
    ) {
        let (win_w, win_h) = window_size;
        if win_w == 0 || win_h == 0 || scale_factor <= 0.0 {
            return;
        }

        // Convert physical touch position to logical points.
        let pos = Pos2::new(
            touch.location.x as f32 / scale_factor,
            touch.location.y as f32 / scale_factor,
        );

        let screen_w = win_w as f32 / scale_factor;
        let screen_h = win_h as f32 / scale_factor;

        let (_, bottom_vp) = layout(win_w, win_h, arrangement);
        let bottom_rect = bottom_vp.map(|vp| {
            Rect::from_min_size(
                Pos2::new(vp.x / scale_factor, vp.y / scale_factor),
                Vec2::new(vp.width / scale_factor, vp.height / scale_factor),
            )
        });

        match touch.phase {
            TouchPhase::Started => {
                let target = self.hit_test(pos, screen_w, screen_h, bottom_rect);
                if let Some(target) = target {
                    if target == TouchTarget::CirclePad {
                        self.stick_touch_id = Some(touch.id);
                        self.update_stick(pos, screen_w, screen_h);
                    } else if target == TouchTarget::Menu {
                        self.toggle_menu = true;
                    } else if target == TouchTarget::BottomScreen {
                        if let Some(rect) = bottom_rect {
                            self.update_bottom_touch(pos, rect);
                        }
                    }
                    self.active_touches.insert(touch.id, (target, pos));
                }
            }
            TouchPhase::Moved => {
                if let Some((target, touch_pos)) = self.active_touches.get_mut(&touch.id) {
                    *touch_pos = pos;
                    if *target == TouchTarget::CirclePad {
                        self.update_stick(pos, screen_w, screen_h);
                    } else if *target == TouchTarget::BottomScreen {
                        if let Some(rect) = bottom_rect {
                            self.update_bottom_touch(pos, rect);
                        }
                    }
                }
            }
            TouchPhase::Ended | TouchPhase::Cancelled => {
                if let Some((target, _)) = self.active_touches.remove(&touch.id) {
                    if target == TouchTarget::CirclePad {
                        self.stick_touch_id = None;
                        self.circle_offset = Vec2::ZERO;
                    } else if target == TouchTarget::BottomScreen {
                        // Check if other fingers are still on bottom screen.
                        let remaining_bottom = self.active_touches.iter().find_map(|(_, (t, p))| {
                            if *t == TouchTarget::BottomScreen {
                                bottom_rect.map(|rect| {
                                    let tx = ((p.x - rect.min.x) / rect.width() * 320.0).clamp(0.0, 319.0) as u16;
                                    let ty = ((p.y - rect.min.y) / rect.height() * 240.0).clamp(0.0, 239.0) as u16;
                                    (tx, ty)
                                })
                            } else {
                                None
                            }
                        });
                        self.bottom_touch = remaining_bottom;
                    }
                }
            }
        }
    }

    fn update_stick(&mut self, pos: Pos2, screen_w: f32, screen_h: f32) {
        let center = self.circle_pad_center(screen_w, screen_h);
        let max_radius = 50.0;
        let delta = pos - center;
        let dist = delta.length();
        if dist > 0.0 {
            let norm = delta / dist;
            let clamped_dist = dist.min(max_radius) / max_radius;
            // 3DS Circle Pad Y is positive UP.
            self.circle_offset = Vec2::new(norm.x * clamped_dist, -norm.y * clamped_dist);
        } else {
            self.circle_offset = Vec2::ZERO;
        }
    }

    fn update_bottom_touch(&mut self, pos: Pos2, rect: Rect) {
        let tx = ((pos.x - rect.min.x) / rect.width() * 320.0).clamp(0.0, 319.0) as u16;
        let ty = ((pos.y - rect.min.y) / rect.height() * 240.0).clamp(0.0, 239.0) as u16;
        self.bottom_touch = Some((tx, ty));
    }

    fn hit_test(&self, pos: Pos2, screen_w: f32, screen_h: f32, bottom_rect: Option<Rect>) -> Option<TouchTarget> {
        let btn_hit = |center: Pos2, radius: f32| pos.distance(center) <= radius;
        let rect_hit = |rect: Rect| rect.contains(pos);

        // 1. Shoulders: L & R
        if rect_hit(self.l_rect(screen_w, screen_h)) {
            return Some(TouchTarget::Button(PadState::L));
        }
        if rect_hit(self.r_rect(screen_w, screen_h)) {
            return Some(TouchTarget::Button(PadState::R));
        }

        // 2. Menu button (center top)
        if rect_hit(self.menu_rect(screen_w, screen_h)) {
            return Some(TouchTarget::Menu);
        }

        // 3. Start & Select buttons (center bottom)
        if rect_hit(self.select_rect(screen_w, screen_h)) {
            return Some(TouchTarget::Button(PadState::SELECT));
        }
        if rect_hit(self.start_rect(screen_w, screen_h)) {
            return Some(TouchTarget::Button(PadState::START));
        }

        // 4. Circle Pad
        let circle_center = self.circle_pad_center(screen_w, screen_h);
        if pos.distance(circle_center) <= 65.0 {
            return Some(TouchTarget::CirclePad);
        }

        // 5. D-Pad (Up, Down, Left, Right)
        let dpad_center = self.dpad_center(screen_w, screen_h);
        let dpad_btn_r = 22.0;
        let dpad_spacing = 30.0;

        if btn_hit(dpad_center + Vec2::new(0.0, -dpad_spacing), dpad_btn_r) {
            return Some(TouchTarget::Button(PadState::UP));
        }
        if btn_hit(dpad_center + Vec2::new(0.0, dpad_spacing), dpad_btn_r) {
            return Some(TouchTarget::Button(PadState::DOWN));
        }
        if btn_hit(dpad_center + Vec2::new(-dpad_spacing, 0.0), dpad_btn_r) {
            return Some(TouchTarget::Button(PadState::LEFT));
        }
        if btn_hit(dpad_center + Vec2::new(dpad_spacing, 0.0), dpad_btn_r) {
            return Some(TouchTarget::Button(PadState::RIGHT));
        }

        // 6. Action buttons: A, B, X, Y (Nintendo 3DS Diamond layout)
        let abxy_center = self.abxy_center(screen_w, screen_h);
        let btn_radius = 24.0;
        let spacing = 36.0;

        if btn_hit(abxy_center + Vec2::new(spacing, 0.0), btn_radius) {
            return Some(TouchTarget::Button(PadState::A));
        }
        if btn_hit(abxy_center + Vec2::new(0.0, spacing), btn_radius) {
            return Some(TouchTarget::Button(PadState::B));
        }
        if btn_hit(abxy_center + Vec2::new(0.0, -spacing), btn_radius) {
            return Some(TouchTarget::Button(PadState::X));
        }
        if btn_hit(abxy_center + Vec2::new(-spacing, 0.0), btn_radius) {
            return Some(TouchTarget::Button(PadState::Y));
        }

        // 7. 3DS Bottom Touch Screen (only if within bounds and not on buttons)
        if let Some(rect) = bottom_rect {
            if rect.contains(pos) {
                return Some(TouchTarget::BottomScreen);
            }
        }

        None
    }

    /// Renders the on-screen touch controls with egui painter.
    pub fn draw(&self, ui: &mut Ui, opacity: f32) {
        let rect = ui.max_rect();
        let screen_w = rect.width();
        let screen_h = rect.height();

        let alpha = (opacity * 255.0).clamp(0.0, 255.0) as u8;
        let bg_color = Color32::from_rgba_unmultiplied(22, 24, 30, (alpha as f32 * 0.6) as u8);
        let border_color = Color32::from_rgba_unmultiplied(210, 220, 235, (alpha as f32 * 0.7) as u8);
        let stroke = Stroke::new(1.5, border_color);
        let text_color = Color32::from_rgba_unmultiplied(245, 245, 250, alpha);

        let painter = ui.painter();

        // 1. Shoulders: L & R
        let l_rect = self.l_rect(screen_w, screen_h);
        let l_bg = if self.is_pressed(PadState::L) {
            Color32::from_rgba_unmultiplied(65, 120, 210, (alpha as f32 * 0.9) as u8)
        } else {
            bg_color
        };
        painter.rect(l_rect, CornerRadius::same(10), l_bg, stroke, StrokeKind::Inside);
        painter.text(l_rect.center(), Align2::CENTER_CENTER, "L", FontId::proportional(16.0), text_color);

        let r_rect = self.r_rect(screen_w, screen_h);
        let r_bg = if self.is_pressed(PadState::R) {
            Color32::from_rgba_unmultiplied(65, 120, 210, (alpha as f32 * 0.9) as u8)
        } else {
            bg_color
        };
        painter.rect(r_rect, CornerRadius::same(10), r_bg, stroke, StrokeKind::Inside);
        painter.text(r_rect.center(), Align2::CENTER_CENTER, "R", FontId::proportional(16.0), text_color);

        // 2. Menu button (center top)
        let menu_rect = self.menu_rect(screen_w, screen_h);
        painter.rect(menu_rect, CornerRadius::same(6), bg_color, stroke, StrokeKind::Inside);
        painter.text(menu_rect.center(), Align2::CENTER_CENTER, "MENU", FontId::proportional(11.0), text_color);

        // 3. Start & Select buttons
        let select_rect = self.select_rect(screen_w, screen_h);
        let select_bg = if self.is_pressed(PadState::SELECT) {
            Color32::from_rgba_unmultiplied(65, 120, 210, (alpha as f32 * 0.9) as u8)
        } else {
            bg_color
        };
        painter.rect(select_rect, CornerRadius::same(6), select_bg, stroke, StrokeKind::Inside);
        painter.text(select_rect.center(), Align2::CENTER_CENTER, "SELECT", FontId::proportional(10.0), text_color);

        let start_rect = self.start_rect(screen_w, screen_h);
        let start_bg = if self.is_pressed(PadState::START) {
            Color32::from_rgba_unmultiplied(65, 120, 210, (alpha as f32 * 0.9) as u8)
        } else {
            bg_color
        };
        painter.rect(start_rect, CornerRadius::same(6), start_bg, stroke, StrokeKind::Inside);
        painter.text(start_rect.center(), Align2::CENTER_CENTER, "START", FontId::proportional(10.0), text_color);

        // 4. Circle Pad (Analog Stick)
        let circle_center = self.circle_pad_center(screen_w, screen_h);
        // Base ring with subtle guide circle
        painter.circle(circle_center, 50.0, bg_color, stroke);
        painter.circle_stroke(circle_center, 28.0, Stroke::new(1.0, Color32::from_rgba_unmultiplied(180, 190, 210, (alpha as f32 * 0.25) as u8)));
        // Knob offset: 3DS Y is up, screen Y is down
        let knob_center = circle_center + Vec2::new(self.circle_offset.x * 28.0, -self.circle_offset.y * 28.0);
        let knob_bg = if self.stick_touch_id.is_some() {
            Color32::from_rgba_unmultiplied(65, 130, 225, (alpha as f32 * 0.95) as u8)
        } else {
            Color32::from_rgba_unmultiplied(48, 54, 66, (alpha as f32 * 0.85) as u8)
        };
        painter.circle(knob_center, 22.0, knob_bg, stroke);

        // 5. D-Pad (Up, Down, Left, Right)
        let dpad_center = self.dpad_center(screen_w, screen_h);
        let dpad_btn_r = 18.0;
        let dpad_spacing = 30.0;
        let dpad_buttons = [
            (PadState::UP, Vec2::new(0.0, -dpad_spacing), "▲"),
            (PadState::DOWN, Vec2::new(0.0, dpad_spacing), "▼"),
            (PadState::LEFT, Vec2::new(-dpad_spacing, 0.0), "◄"),
            (PadState::RIGHT, Vec2::new(dpad_spacing, 0.0), "►"),
        ];
        for (btn, offset, label) in dpad_buttons {
            let center = dpad_center + offset;
            let bg = if self.is_pressed(btn) {
                Color32::from_rgba_unmultiplied(65, 120, 210, (alpha as f32 * 0.9) as u8)
            } else {
                bg_color
            };
            painter.circle(center, dpad_btn_r, bg, stroke);
            painter.text(center, Align2::CENTER_CENTER, label, FontId::proportional(12.0), text_color);
        }

        // 6. Action buttons (A, B, X, Y) with authentic Nintendo 3DS colors
        let abxy_center = self.abxy_center(screen_w, screen_h);
        let btn_radius = 21.0;
        let spacing = 36.0;
        let action_buttons = [
            (PadState::A, Vec2::new(spacing, 0.0), "A", Color32::from_rgb(225, 75, 75)),  // Nintendo Red
            (PadState::B, Vec2::new(0.0, spacing), "B", Color32::from_rgb(235, 180, 45)), // Nintendo Yellow
            (PadState::X, Vec2::new(0.0, -spacing), "X", Color32::from_rgb(60, 135, 235)),// Nintendo Blue
            (PadState::Y, Vec2::new(-spacing, 0.0), "Y", Color32::from_rgb(50, 185, 95)), // Nintendo Green
        ];
        for (btn, offset, label, accent) in action_buttons {
            let center = abxy_center + offset;
            let pressed = self.is_pressed(btn);
            let bg = if pressed {
                Color32::from_rgba_unmultiplied(accent.r(), accent.g(), accent.b(), (alpha as f32 * 0.95) as u8)
            } else {
                bg_color
            };
            let btn_stroke = if pressed {
                Stroke::new(2.0, Color32::WHITE)
            } else {
                Stroke::new(1.5, Color32::from_rgba_unmultiplied(accent.r(), accent.g(), accent.b(), (alpha as f32 * 0.8) as u8))
            };
            painter.circle(center, btn_radius, bg, btn_stroke);
            painter.text(center, Align2::CENTER_CENTER, label, FontId::proportional(15.0), text_color);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use winit::dpi::PhysicalPosition;
    use winit::event::DeviceId;

    fn make_touch(id: u64, x: f64, y: f64, phase: TouchPhase) -> Touch {
        Touch {
            device_id: DeviceId::dummy(),
            phase,
            location: PhysicalPosition::new(x, y),
            force: None,
            id,
        }
    }

    #[test]
    fn touch_controls_trigger_buttons() {
        let mut controls = TouchControls::new();
        // Screen size: 800x400 points, scale 1.0.
        let abxy = controls.abxy_center(800.0, 400.0);
        let a_pos = abxy + Vec2::new(36.0, 0.0);
        let touch_a = make_touch(1, a_pos.x as f64, a_pos.y as f64, TouchPhase::Started);
        controls.handle_touch(&touch_a, (800, 400), 1.0, ScreenLayout::Stacked);
        assert!(controls.is_pressed(PadState::A));

        let input = controls.apply(InputState::default());
        assert!(input.buttons.contains(PadState::A));

        // Release touch
        let release_a = make_touch(1, a_pos.x as f64, a_pos.y as f64, TouchPhase::Ended);
        controls.handle_touch(&release_a, (800, 400), 1.0, ScreenLayout::Stacked);
        assert!(!controls.is_pressed(PadState::A));
        let input2 = controls.apply(InputState::default());
        assert!(!input2.buttons.contains(PadState::A));
    }

    #[test]
    fn touch_controls_circle_pad_analog() {
        let mut controls = TouchControls::new();
        let cp_center = controls.circle_pad_center(800.0, 400.0);
        let touch_start = make_touch(2, cp_center.x as f64, cp_center.y as f64, TouchPhase::Started);
        controls.handle_touch(&touch_start, (800, 400), 1.0, ScreenLayout::Stacked);

        let touch_move = make_touch(2, (cp_center.x + 25.0) as f64, cp_center.y as f64, TouchPhase::Moved);
        controls.handle_touch(&touch_move, (800, 400), 1.0, ScreenLayout::Stacked);

        let input = controls.apply(InputState::default());
        assert!(input.circle_x > 0.4 && input.circle_x < 0.6);
        assert_eq!(input.circle_y, 0.0);

        let touch_end = make_touch(2, (cp_center.x + 25.0) as f64, cp_center.y as f64, TouchPhase::Ended);
        controls.handle_touch(&touch_end, (800, 400), 1.0, ScreenLayout::Stacked);

        let input_after = controls.apply(InputState::default());
        assert_eq!(input_after.circle_x, 0.0);
        assert_eq!(input_after.circle_y, 0.0);
    }
}
