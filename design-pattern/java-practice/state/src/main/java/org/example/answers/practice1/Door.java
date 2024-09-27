package org.example.answers.practice1;

import org.example.answers.practice1.state.DoorState;
import org.example.answers.practice1.state.Closed;

public class Door {
  private DoorState state;

  public Door() {
    // 初期状態は閉じている
    this.state = new Closed();
  }

  public void open() {
    this.state = this.state.open();
  }

  public void close() {
    this.state = this.state.close();
  }

  public void lock() {
    this.state = this.state.lock();
  }

  public void unlock() {
    this.state = this.state.unlock();
  }

  public String getStatus() {
    System.out.println(this.state.getStatus());
    return this.state.getStatus();
  }
}
