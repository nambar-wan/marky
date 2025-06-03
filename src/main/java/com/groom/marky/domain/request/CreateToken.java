package com.groom.marky.domain.request;

import com.groom.marky.domain.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateToken {

	private String userEmail;
	private Role role;
	private String ip;
	private String userAgent;
}
